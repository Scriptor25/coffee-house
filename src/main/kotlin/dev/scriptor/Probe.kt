package dev.scriptor

import dev.scriptor.model.ffmpeg.*
import java.io.BufferedReader
import java.util.logging.Logger

class Probe(
    private val log: Logger,
    private val ffmpeg: String = "ffmpeg",
) {
    operator fun invoke() = probe()

    private fun probe(): Capabilities {
        log.fine("probe device types")

        val deviceTypes = probeDeviceTypes()

        log.fine("probe codecs")

        val codecs = probeCodecs()

        log.fine("probe decoders")

        val decoders = probeDecoders()

        log.fine("probe encoders")

        val encoders = probeEncoders()

        log.fine("probe filters")

        val filters = probeFilters()

        log.fine("probe devices")

        val devices = deviceTypes.mapNotNull(::probeDevice)

        log.fine("probe interop")

        val interop = probeInterop(devices)

        return Capabilities(
            devices.associateBy(DeviceCapabilities::id),
            codecs.associateBy(CodecCapabilities::id),
            decoders.associateBy(ImplementationCapabilities::id),
            encoders.associateBy(ImplementationCapabilities::id),
            filters.associateBy(FilterCapabilities::id),
            interop.associateBy { it.src to it.dst },
        )
    }

    private fun probeDeviceTypes(): Set<DeviceId> {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "list",
        )

        if (result.error) {
            error("failed to probe device types:\n${result.stderr}")
        }

        return result.stdout
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .drop(1)
            .map(::DeviceId)
            .toSet()
    }

    private fun probeCodecs(): List<CodecCapabilities> {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-codecs",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            error("failed to probe encoders:\n$message")
        }

        return parseCodecs(result.stdout)
    }

    private fun probeEncoders(): List<ImplementationCapabilities> {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-encoders",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            error("failed to probe encoders:\n$message")
        }

        return parseImplementation(result.stdout, CodecDirection.ENCODE)
    }

    private fun probeDecoders(): List<ImplementationCapabilities> {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-decoders",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            error("failed to probe decoders:\n$message")
        }

        return parseImplementation(result.stdout, CodecDirection.DECODE)
    }

    private fun probeFilters(): List<FilterCapabilities> {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-filters",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            error("failed to probe filters:\n$message")
        }

        return parseFilters(result.stdout)
    }

    private fun probeDevice(
        id: DeviceId,
    ): DeviceCapabilities? {
        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$id=probe",
            "-filter_hw_device", "probe",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-vf", "format=nv12,hwupload,hwdownload,format=nv12",
            "-frames:v", "1",
            "-f", "null",
            "-",
        )

        if (result.error) {
            return null
        }

        return DeviceCapabilities(
            id,
        )
    }

    private fun probeInterop(devices: List<DeviceCapabilities>): List<InteropCapabilities> {
        val result = mutableListOf<InteropCapabilities>()

        for (src in devices) {
            for (dst in devices) {
                if (src == dst) {
                    result += InteropCapabilities(
                        src = src.id,
                        dst = dst.id,
                        derivable = true,
                        direct = true,
                        mapping = true,
                    )
                    continue
                }

                result += probeDeviceInterop(src.id, dst.id) ?: continue
            }
        }

        return result
    }

    private fun probeDeviceInterop(src: DeviceId, dst: DeviceId): InteropCapabilities? {
        log.finer("probe device interop: $src ---> $dst")

        val resultDerive = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwdownload,format=nv12",
            "-f", "null",
            "-",
        )

        if (resultDerive.error) {
            return null
        }

        val resultDirect = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=$dst:direct=1,hwdownload,format=nv12",
            "-f", "null",
            "-",
        )

        if (resultDirect.success) {
            return InteropCapabilities(
                src = src,
                dst = dst,
                derivable = true,
                direct = true,
                mapping = true,
            )
        }

        val resultMapping = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=$dst,hwdownload,format=nv12",
            "-f", "null",
            "-",
        )

        if (resultMapping.success) {
            return InteropCapabilities(
                src = src,
                dst = dst,
                derivable = true,
                direct = false,
                mapping = true,
            )
        }

        return InteropCapabilities(
            src = src,
            dst = dst,
            derivable = true,
            direct = false,
            mapping = false,
        )
    }

    private val codecLineRegex = """^\s*([DEVASTIL.]{6})\s+(\S+)\s+(.*)$""".toRegex()
    private val codecDecodersRegex = """\(decoders:\s*(.+)\)""".toRegex()
    private val codecEncodersRegex = """\(encoders:\s*(.+)\)""".toRegex()

    private fun parseCodecs(text: String): List<CodecCapabilities> {
        val result = mutableListOf<CodecCapabilities>()

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = codecLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]
            val description = match.groupValues[3]

            val type = when (flags[2]) {
                'V' -> CodecType.VIDEO
                'A' -> CodecType.AUDIO
                'S' -> CodecType.SUBTITLE
                'D' -> CodecType.DATA
                'T' -> CodecType.ATTACHMENT
                else -> null
            } ?: continue

            val id = CodecId(name)

            val decoders = codecDecodersRegex
                .findAll(description)
                .flatMap { it.groupValues[1].split("\\s+".toRegex()) }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::ImplementationId)
                .toSet()

            val encoders = codecEncodersRegex
                .findAll(description)
                .flatMap { it.groupValues[1].split("\\s+".toRegex()) }
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(::ImplementationId)
                .toSet()

            result += CodecCapabilities(
                id,
                type,
                flags[0] == 'D',
                flags[1] == 'E',
                flags[3] == 'I',
                flags[4] == 'L',
                flags[5] == 'S',
                decoders,
                encoders,
            )
        }

        return result
    }

    private val coderGeneralCapabilitiesRegex = """General capabilities:\s*(.+)""".toRegex()
    private val coderThreadingCapabilitiesRegex = """Threading capabilities:\s*(.+)""".toRegex()
    private val coderSupportedHardwareDevicesRegex = """Supported hardware devices:\s*(.+)""".toRegex()
    private val coderSupportedPixelFormatsRegex = """Supported pixel formats:\s*(.+)""".toRegex()
    private val coderSupportedSampleRatesRegex = """Supported sample rates:\s*(.+)""".toRegex()
    private val coderSupportedSampleFormatsRegex = """Supported sample formats:\s*(.+)""".toRegex()
    private val coderSupportedChannelLayoutsRegex = """Supported channel layouts:\s*(.+)""".toRegex()

    private fun parseCoderCapabilities(regex: Regex, text: String): Set<String> {
        val match = regex.find(text)

        return if (match != null) {
            match.groupValues[1]
                .split("\\s+".toRegex())
                .filter(String::isNotBlank)
                .toSet()
        } else emptySet()
    }

    private fun probeCoderCapabilities(coder: ImplementationCapabilities): ImplementationCapabilities {
        val kind = when (coder.direction) {
            CodecDirection.DECODE -> "decoder"
            CodecDirection.ENCODE -> "encoder"
        }

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-h", "$kind=${coder.id}",
        )

        val generalCapabilities = parseCoderCapabilities(coderGeneralCapabilitiesRegex, result.stdout)
        val threadingCapabilities = parseCoderCapabilities(coderThreadingCapabilitiesRegex, result.stdout)
        val supportedHardwareDevices = parseCoderCapabilities(coderSupportedHardwareDevicesRegex, result.stdout)
        val supportedPixelFormats = parseCoderCapabilities(coderSupportedPixelFormatsRegex, result.stdout)
        val supportedSampleRates = parseCoderCapabilities(coderSupportedSampleRatesRegex, result.stdout)
        val supportedSampleFormats = parseCoderCapabilities(coderSupportedSampleFormatsRegex, result.stdout)
        val supportedChannelLayouts = parseCoderCapabilities(coderSupportedChannelLayoutsRegex, result.stdout)

        return coder.copy(
            generalCapabilities = generalCapabilities,
            threadingCapabilities = threadingCapabilities,
            supportedHardwareDevices = supportedHardwareDevices.map(::DeviceId).toSet(),
            supportedPixelFormats = supportedPixelFormats,
            supportedSampleRates = supportedSampleRates.map(String::toLong).toSet(),
            supportedSampleFormats = supportedSampleFormats,
            supportedChannelLayouts = supportedChannelLayouts,
        )
    }

    private val coderLineRegex = """^\s*([VASFXBD.]{6})\s+(\S+)\s+(.*)$""".toRegex()

    private fun parseImplementation(text: String, direction: CodecDirection): List<ImplementationCapabilities> {
        val result = mutableListOf<ImplementationCapabilities>()

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = coderLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]

            val id = ImplementationId(name)

            val type = when (flags[0]) {
                'V' -> CodecType.VIDEO
                'A' -> CodecType.AUDIO
                'S' -> CodecType.SUBTITLE
                else -> null
            } ?: continue

            result += ImplementationCapabilities(
                id,
                type,
                direction,
                flags[1] == 'F',
                flags[2] == 'S',
                flags[3] == 'X',
                flags[4] == 'B',
                flags[5] == 'D',
            )
        }

        return result.map(::probeCoderCapabilities)
    }

    private val filterLineRegex = """^\s*([TS.]{2,3})\s+(\S+)\s+([AVN|\->]+)\s+(.+)$""".toRegex()

    private fun parseFilters(text: String): List<FilterCapabilities> {
        val result = mutableListOf<FilterCapabilities>()

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = filterLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]
            val transform = match.groupValues[3]

            val id = FilterId(name)

            result += FilterCapabilities(
                id,
                transform,
                flags[0] == 'T',
                flags[1] == 'S',
            )
        }

        return result
    }

    private data class CommandResult(
        val code: Int,
        val stdout: String,
        val stderr: String,
    ) {
        val success: Boolean
            get() = code == 0

        val error: Boolean
            get() = code != 0
    }

    private fun command(vararg args: String): CommandResult {
        val process = ProcessBuilder(ffmpeg, *args).start()

        val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)

        val code = process.waitFor()

        return CommandResult(code, stdout, stderr)
    }
}
