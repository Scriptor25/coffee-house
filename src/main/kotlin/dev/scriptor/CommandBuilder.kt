package dev.scriptor

import dev.scriptor.encoder.audio.AudioEncoder
import dev.scriptor.encoder.subtitle.SubtitleEncoder
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class CommandBuilder(
    val ffmpeg: String,
    val enable: Boolean,
    val device: String?,
    val input: Path,
    val cache: Path,
    val outputs: List<Output>,
    val pipeline: Pipeline,
) {

    fun build(): List<String> = buildList {
        this += listOf(
            ffmpeg,
            "-hide_banner",
            "-y", // allow overriding existing files
        )

        this += buildDevices()
        this += buildDecode()
        this += buildFilter()
        this += buildStreamMap()
        this += buildEncode()
        this += buildMuxer()
    }

    private fun buildDevices(): List<String> {
        if (!enable) {
            return emptyList()
        }

        if (device == null) {
            return pipeline.devices.flatMap { listOf("-init_hw_device", "$it") }
        }

        return pipeline.devices.flatMap { listOf("-init_hw_device", "$it:$device") }
    }

    private fun buildDecode(): List<String> {
        if (!enable) {
            return listOf("-i", input.absolutePathString())
        }

        return pipeline.decode.decoder(0) + listOf("-i", input.absolutePathString())
    }

    private fun buildFilter(): List<String> {
        val video = outputs.filterIsInstance<VideoOutput>()

        if (!enable || video.isEmpty()) {
            return emptyList()
        }

        val input = video.first().source
        val count = video.size

        val filter = buildString {
            append("[0:$input]")
            append(pipeline.split(count).joinToString(","))

            video.indices.forEach { append("[v$it]") }

            video.forEachIndexed { index, output ->
                val transform =
                    if (!output.scaled) pipeline.encode()
                    else pipeline.scaleEncode(output.width, output.height)

                append(";")
                append("[v$index]")
                append(transform.ifEmpty { listOf("null") }.joinToString(","))
                append("[s$index]")
            }
        }

        return listOf("-filter_complex", filter)
    }

    private fun buildStreamMap(): List<String> {
        var filtered = 0

        return outputs.flatMap {
            when (it) {
                is VideoOutput -> listOf("-map", if (enable) "[s${filtered++}]" else "0:${it.source}")
                is AudioOutput -> listOf("-map", "0:${it.source}")
                is SubtitleOutput -> listOf("-map", "0:${it.source}")
            }
        }
    }

    private fun buildEncode(): List<String> {
        var video = 0
        var audio = 0
        var subtitle = 0

        return outputs.flatMap {
            when (it) {
                is VideoOutput -> buildVideoEncode(video++, it)
                is AudioOutput -> buildAudioEncode(audio++, it)
                is SubtitleOutput -> buildSubtitleEncode(subtitle++, it)
            }
        }
    }

    private fun buildVideoEncode(index: Int, output: VideoOutput): List<String> =
        output.encoding(index, pipeline.encode.encoder)

    private fun buildAudioEncode(index: Int, output: AudioOutput): List<String> =
        output.encoding(index, AudioEncoder.Aac) // TODO: dont hardcode encoder

    private fun buildSubtitleEncode(index: Int, output: SubtitleOutput): List<String> =
        output.encoding(index, SubtitleEncoder.WebVtt) // TODO: dont hardcode encoder

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

    private fun buildMuxer(): List<String> {
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
