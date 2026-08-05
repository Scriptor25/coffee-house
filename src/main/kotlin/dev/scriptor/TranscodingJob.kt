package dev.scriptor

import dev.scriptor.model.MediaMetadata
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

data class TranscodingJob(
    val item: MediaMetadata,
    val cache: Path,
    val variants: List<Variant>,
    val transcoding: Boolean,
) {
    private val command = buildCommand()

    @Volatile
    private var process: Process? = null

    @Synchronized
    context(log: Logger)
    fun start() {
        if (process?.isAlive == true) return

        cache.createDirectories()

        log.fine(command.joinToString("' '", "'", "'"))

        val p = ProcessBuilder(command).start()
        p.attach(log)
        process = p
    }

    context(log: Logger)
    fun waitFor(path: Path): Path {
        start()

        while (path.notExists()) {
            val p = process!!

            if (!p.isAlive) {
                error("ffmpeg exited with code ${p.exitValue()}")
            }

            Thread.sleep(10)
        }

        return path
    }

    context(log: Logger)
    fun master(): Path = waitFor(cache.resolve("master.m3u8"))

    context(log: Logger)
    fun index(name: String): Path = waitFor(cache.resolve(name).resolve("index.m3u8"))

    context(log: Logger)
    fun segment(name: String, index: Long): Path = waitFor(cache.resolve(name).resolve("segment$index.ts"))

    private fun buildCommand(): List<String> {
        val transcoded = if (transcoding)
            variants.filter { it.name != "original" }
        else emptyList()

        val filter = if (transcoded.isEmpty()) null else {
            buildString {
                append("[0:v]split=${transcoded.size}")
                transcoded.indices.forEach { append("[v$it]") }

                transcoded.forEachIndexed { index, variant ->
                    append(";")
                    append("[v$index]")
                    append("scale=${variant.width}:${variant.height}")
                    append("[s$index]")
                }
            }
        }

        val command = mutableListOf(
            "ffmpeg",
            "-y", // allow overriding existing files
            "-i", item.path.absolutePathString(),
        )

        if (filter != null) {
            command += listOf("-filter_complex", filter)
        }

        command += listOf(
            "-map", "0:v",
            "-map", "0:a",
        )

        transcoded.forEachIndexed { index, _ ->
            command += listOf(
                "-map", "[s$index]",
                "-map", "0:a",
            )
        }

        command += listOf(
            "-c:v:0", "copy",
            "-c:a:0", "copy",
        )

        transcoded.forEachIndexed { index, variant ->
            val stream = index + 1

            command += listOf(
                "-c:v:$stream", "libx264",
                "-preset:v:$stream", variant.profile.preset,
                "-crf:v:$stream", variant.profile.crf.toString(),

                "-maxrate:v:$stream", variant.bitrate.toString(),
                "-bufsize:v:$stream", (variant.bitrate * 2).toString(),
                "-b:v:$stream", variant.bitrate.toString(),

                "-c:a:$stream", "aac",
            )
        }

        val map = buildString {
            append("v:0,a:0,name:original")

            transcoded.forEachIndexed { index, variant ->
                val stream = index + 1

                append(" v:$stream,a:$stream,name:${variant.name}")
            }
        }

        command += listOf(
            "-f", "hls",
            "-var_stream_map", map,
            "-master_pl_name", "master.m3u8",
            "-hls_time", "4",
            // "-hls_playlist_type", "vod", // TODO: check if this is required
            "-hls_flags", "independent_segments",
            "-hls_segment_filename", cache.resolve("%v/segment%d.ts").absolutePathString(),
            cache.resolve("%v/index.m3u8").absolutePathString(),
        )

        return command
    }
}
