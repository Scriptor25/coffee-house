package dev.scriptor

import java.nio.file.Path
import kotlin.io.path.absolutePathString

class Ffmpeg(
    private val input: Path,
    private val cache: Path,
    private val outputs: List<Output>,
) {
    fun build(): List<String> =
        buildList {
            this += listOf(
                "ffmpeg",
                "-y", // allow overriding existing files
                "-i", input.absolutePathString(),
            )

            this += buildFilter()
            this += buildMappings()
            this += buildCodecs()
            this += buildHls()
        }

    private fun buildFilter(): List<String> {
        val scaled = outputs.filterIsInstance<VideoOutput>().filter { it.scaled }

        if (scaled.isEmpty()) {
            return emptyList()
        }

        val input = scaled.first().source
        val count = scaled.size

        val filter = buildString {
            append("[0:$input]")
            append("split=$count")

            scaled.indices.forEach { append("[v$it]") }

            scaled.forEachIndexed { index, output ->
                val width = output.width
                val height = output.height

                append(";")
                append("[v$index]")
                append("scale=$width:$height")
                append("[s$index]")
            }
        }

        return listOf("-filter_complex", filter)
    }

    private fun buildMappings(): List<String> {
        var filtered = 0

        return outputs.flatMap {
            when (it) {
                is VideoOutput ->
                    if (it.scaled)
                        listOf("-map", "[s${filtered++}]")
                    else
                        listOf("-map", "0:${it.source}")

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
        when (val codec = output.codec) {
            is VideoCodec.Copy -> listOf(
                "-c:v:$index", "copy",
            )

            is VideoCodec.H264 -> listOf(
                "-c:v:$index", "libx264",

                "-preset:v:$index", codec.profile.preset,
                "-crf:v:$index", codec.profile.crf.toString(),

                "-b:v:$index", codec.bitrate.toString(),

                "-maxrate:v:$index", codec.bitrate.toString(),
                "-bufsize:v:$index", (codec.bitrate * 2).toString(),
            )
        }

    private fun buildAudioCodec(index: Int, output: AudioOutput): List<String> =
        when (output.codec) {
            is AudioCodec.Copy -> listOf(
                "-c:a:$index", "copy",
            )

            is AudioCodec.Aac -> listOf(
                "-c:a:$index", "aac",
            )
        }

    private fun buildSubtitleCodec(index: Int, output: SubtitleOutput): List<String> =
        when (output.codec) {
            is SubtitleCodec.Copy -> listOf(
                "-c:s:$index", "copy",
            )

            is SubtitleCodec.WebVtt -> listOf(
                "-c:s:$index", "webvtt",
            )
        }

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
            "-hls_fmp4_init_filename", "init.mp4",
            "-hls_segment_filename", cache.resolve("%v/segment%d.mp4").absolutePathString(),
            cache.resolve("%v/index.m3u8").absolutePathString(),
        )
    }
}

fun ffmpeg(
    input: Path,
    cache: Path,
    outputs: List<Output>,
): List<String> = Ffmpeg(input, cache, outputs).build()
