package dev.scriptor

import dev.scriptor.backend.SoftwareVideoBackend
import dev.scriptor.backend.VideoBackend
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class Ffmpeg(
    private val input: Path,
    private val cache: Path,
    private val outputs: List<Output>,
    private val decode: VideoBackend,
    private val scale: VideoBackend,
    private val encode: VideoBackend,
) {
    fun build(): List<String> = buildList {
        this += listOf("ffmpeg", "-hide_banner")

        val devices = mutableSetOf<String>()

        if (decode !is SoftwareVideoBackend) {
            devices += decode.name
        }

        if (encode !is SoftwareVideoBackend) {
            devices += encode.name
        }

        devices.forEach { this += listOf("-init_hw_device", it) }

        if (decode !is SoftwareVideoBackend) {
            this += listOf("-hwaccel", decode.name)
            this += listOf("-hwaccel_output_format", decode.name)
        }

        this += "-y" // allow overriding existing files
        this += listOf("-i", input.absolutePathString())

        this += buildFilter()
        this += buildMappings()
        this += buildCodecs()
        this += buildHls()
    }

    private fun buildFilter(): List<String> {
        val video = outputs.filterIsInstance<VideoOutput>()

        if (video.isEmpty()) {
            return emptyList()
        }

        val input = video.first().source
        val count = video.size

        val filter = buildString {
            append("[0:$input]")
            append("split=$count")

            video.indices.forEach { append("[v$it]") }

            val pipeline = Pipeline(
                10, // TODO
                decode,
                scale,
                encode,
            )

            video.forEachIndexed { index, output ->
                val transform = if (output.scaled) {
                    val width = output.width
                    val height = output.height

                    pipeline.build(width, height)
                } else pipeline.build()

                append(";")
                append("[v$index]")
                append(transform)
                append("[s$index]")
            }
        }

        return listOf("-filter_complex", filter)
    }

    private fun buildMappings(): List<String> {
        var filtered = 0

        return outputs.flatMap {
            when (it) {
                is VideoOutput -> listOf("-map", "[s${filtered++}]")
                is AudioOutput -> listOf("-map", "0:${it.source}")
                is SubtitleOutput -> listOf("-map", "0:${it.source}")
            }
        }
    }

    private fun buildCodecs(): List<String> {
        var video = 0
        var audio = 0
        var subtitle = 0

        return outputs.flatMap {
            when (it) {
                is VideoOutput -> buildVideoCodec(video++, it)
                is AudioOutput -> buildAudioCodec(audio++, it)
                is SubtitleOutput -> buildSubtitleCodec(subtitle++, it)
            }
        }
    }

    private fun buildVideoCodec(index: Int, output: VideoOutput): List<String> =
        output.encoding(index, encode)

    private fun buildAudioCodec(index: Int, output: AudioOutput): List<String> =
        output.encoding(index)

    private fun buildSubtitleCodec(index: Int, output: SubtitleOutput): List<String> =
        output.encoding(index)

    private fun buildVariantMap(): String {
        var video = 0
        var audio = 0
        var subtitle = 0

        val hasAudio = outputs.any { it is AudioOutput }
        val hasSubtitles = outputs.any { it is SubtitleOutput }

        val hasDefaultAudio = outputs.filterIsInstance<AudioOutput>().any { it.default }

        return outputs.joinToString(" ") {
            val segments = when (it) {
                is VideoOutput -> listOfNotNull(
                    "v:${video++}",
                    if (hasAudio) "agroup:audio" else null,
                    if (hasSubtitles) "sgroup:subs" else null,
                    "name:${it.name}",
                )

                is AudioOutput -> listOfNotNull(
                    "a:${audio++}",
                    "agroup:audio",
                    if (it.language != null) "language:${it.language}" else null,
                    "name:${it.name}",
                    "default:${if (it.default || (!hasDefaultAudio && audio == 1)) "yes" else "no"}",
                )

                is SubtitleOutput -> listOfNotNull(
                    "s:${subtitle++}",
                    "sgroup:subs",
                    if (it.language != null) "language:${it.language}" else null,
                    "name:${it.name}",
                    "default:${if (it.default) "yes" else "no"}",
                )
            }

            segments.joinToString(",")
        }
    }

    private fun buildHls(): List<String> {
        val variantMap = buildVariantMap()

        return listOf(
            "-f", "hls",
            "-var_stream_map", variantMap,
            "-master_pl_name", "master.m3u8",
            "-copyts",
            "-start_at_zero",
            "-hls_time", "6",
            "-hls_list_size", "0",
            "-hls_flags", "independent_segments+temp_file",
            "-hls_segment_type", "fmp4",
            "-hls_segment_filename", cache.resolve("%v/segment%d.mp4").absolutePathString(),
            cache.resolve("%v/index.m3u8").absolutePathString(),
        )
    }
}

fun ffmpeg(
    input: Path,
    cache: Path,
    outputs: List<Output>,
    decode: VideoBackend,
    scale: VideoBackend,
    encode: VideoBackend,
): List<String> = Ffmpeg(
    input,
    cache,
    outputs,
    decode,
    scale,
    encode,
).build()
