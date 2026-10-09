package dev.scriptor

import dev.scriptor.server.Provider
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.*

context(log: Logger, provider: Provider)
fun convertBitmapToTextSubtitles(input: Path, output: Path) {
    output.createParentDirectories()

    val ffmpeg: String = provider.getT("ffmpeg")
        ?: error("ffmpeg not set")

    val work = createTempDirectory("bitmap-subtitles-")
    val subtitles = (work / "subtitles").createDirectory()

    start(
        "seconv",
        input.absolutePathString(),
        "subrip",
        "--ocr-engine:tesseract",
        "--output-folder:${subtitles.absolutePathString()}",
        "--overwrite"
    ).use { process ->
        val code = process.waitFor()
        if (code != 0) {
            log.warning("failed to convert bitmap to text subtitles")
            return
        }
    }

    val paths = subtitles
        .listDirectoryEntries()
        .filter { path -> path.isRegularFile() && path.name.endsWith(".srt", ignoreCase = true) }
        .sorted()

    val command = mutableListOf(
        ffmpeg,
        "-hide_banner",
        "-i", input.absolutePathString(),
    )

    for (path in paths) {
        command += listOf("-i", path.absolutePathString())
    }

    command += listOf("-map", "0", "-map", "0:s")

    for (i in paths.indices) {
        command += listOf("-map", "${i + 1}:0")
    }

    command += listOf(
        "-map_metadata", "0",
        "-map_chapters", "0",
        "-c", "copy",
        "-c:s", "srt",
        "-y", output.absolutePathString(),
    )

    start(command).use { process ->
        val code = process.waitFor()
        if (code != 0) {
            log.warning("failed to merge text subtitles into output file")
            return
        }
    }
}
