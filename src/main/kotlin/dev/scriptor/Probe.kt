package dev.scriptor

import java.io.BufferedReader


class FfmpegProbe(
    private val ffmpeg: String = "ffmpeg",
) {
    operator fun invoke() = probe()

    private fun probe(): FfmpegCapabilities {
        val version = command("-version")
        val versionLine = version.stdout.lineSequence().firstOrNull()?.trim() ?: "unknown"

        val configuration = parseConfiguration(version.stdout)

        val deviceTypes = probeDeviceTypes()

        val encoders = parseCodecs(command("-hide_banner", "-encoders").stdout)
        val decoders = parseCodecs(command("-hide_banner", "-decoders").stdout)
        val filters = parseCodecs(command("-hide_banner", "-filters").stdout)

        val devices = deviceTypes.associateWith { type ->
            probeDevice(
                type,
                encoders,
                decoders,
                filters,
            )
        }
    }

    private fun parseConfiguration(text: String){}

    private fun parseCodecs(text: String) {}

    private data class CommandResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
    )

    private fun command(vararg args: String): CommandResult {
        val process = ProcessBuilder(ffmpeg, *args).start()

        val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)

        val code = process.waitFor()

        return CommandResult(code, stdout, stderr)
    }
}
