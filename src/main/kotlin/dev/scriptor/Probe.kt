package dev.scriptor

import dev.scriptor.model.ffmpeg.*
import java.io.BufferedReader
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class Probe(
    private val log: Logger,
    private val ffmpeg: String = "ffmpeg",
    private val device: String? = null,
) {
    operator fun invoke() = probe()

    private fun device(type: DeviceId, name: String? = null, inherit: String? = null): String = buildString {
        append(type)
        if (name != null) {
            append("=")
            append(name)
        }
        if (inherit != null) {
            append("@")
            append(inherit)
        } else if (device != null) {
            append(":")
            append(device)
        }
    }

    private fun probe(): Capabilities {
        val deviceIds = probeDevices()
        val codecs = probeCodecs()
        val filters = probeFilters()

        val decoders = probeDecoders()
        val encoders = probeEncoders()

        val devices = deviceIds.mapNotNull(::probeDevice)
        val interop = probeInterop(devices)
        val implementations = probeImplementations(decoders + encoders)

        return Capabilities(
            devices.associateBy(DeviceCapabilities::id),
            interop.associateBy { it.src to it.dst },
            implementations.associateBy(ImplementationCapabilities::id),
            codecs.associateBy(CodecCapabilities::id),
            filters.associateBy(FilterCapabilities::id),
        )
    }

    private fun probeDevices(): Set<DeviceId> {
        log.fine("probe devices")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "list",
        )

        if (result.error) {
            log.fine("failed to probe device types:\n${result.stderr}")
            return emptySet()
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
        log.fine("probe codecs")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-codecs",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe encoders:\n$message")
            return emptyList()
        }

        return parseCodecs(result.stdout)
    }

    private fun probeFilters(): List<FilterCapabilities> {
        log.fine("probe filters")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-filters",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe filters:\n$message")
            return emptyList()
        }

        return parseFilters(result.stdout)
    }

    private fun probeEncoders(): List<ImplementationCapabilities> {
        log.fine("probe encoders")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-encoders",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe encoders:\n$message")
            return emptyList()
        }

        return parseImplementations(result.stdout, CodecDirection.ENCODE)
    }

    private fun probeDecoders(): List<ImplementationCapabilities> {
        log.fine("probe decoders")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-decoders",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe decoders:\n$message")
            return emptyList()
        }

        return parseImplementations(result.stdout, CodecDirection.DECODE)
    }

    private fun probeImplementations(implementations: List<ImplementationCapabilities>): List<ImplementationCapabilities> {
        log.fine("probe implementations (${implementations.size})")

        val executor = Executors.newFixedThreadPool(
            minOf(4, Runtime.getRuntime().availableProcessors()),
        )

        val result = arrayOfNulls<ImplementationCapabilities>(implementations.size)
        for ((index, implementation) in implementations.withIndex()) {
            executor.execute { result[index] = probeImplementation(implementation) }
        }

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)

        return result.filterNotNull()
    }

    private fun probeDevice(
        id: DeviceId,
    ): DeviceCapabilities? {
        log.fine("probe device $id")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", device(id, "probe"),
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
        log.fine("probe device interop $src ---> $dst")

        val resultDerive = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", device(src, "src"),
            "-init_hw_device", device(dst, "dst", "src"),
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
            "-init_hw_device", device(src, "src"),
            "-init_hw_device", device(dst, "dst", "src"),
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=dst:mode=direct,hwdownload,format=nv12",
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
            "-init_hw_device", device(src, "src"),
            "-init_hw_device", device(dst, "dst", "src"),
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "nullsrc",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=dst,hwdownload,format=nv12",
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

    private fun parseSpaceSeparatedSequence(regex: Regex, text: String): Sequence<String> {
        return regex
            .findAll(text)
            .map { it.groupValues[1] }
            .flatMap { it.split("\\s+".toRegex()) }
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    private val codecLineRegex = """^\s*([DEVASTIL.]{6})\s+(\S+)\s+(.*)$""".toRegex()
    private val codecDecodersRegex = """\(decoders:\s*([^)]+)\)""".toRegex()
    private val codecEncodersRegex = """\(encoders:\s*([^)]+)\)""".toRegex()

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

            val decoders = parseSpaceSeparatedSequence(codecDecodersRegex, description)
            val encoders = parseSpaceSeparatedSequence(codecEncodersRegex, description)

            result += CodecCapabilities(
                id,
                type,
                flags[0] == 'D',
                flags[1] == 'E',
                flags[3] == 'I',
                flags[4] == 'L',
                flags[5] == 'S',
                decoders.map(::ImplementationId).toSet(),
                encoders.map(::ImplementationId).toSet(),
            )
        }

        return result
    }

    private val implementationGeneralCapabilitiesRegex = """General capabilities:([^\n]*)""".toRegex()
    private val implementationSupportedHardwareDevicesRegex = """Supported hardware devices:([^\n]*)""".toRegex()
    private val implementationSupportedPixelFormatsRegex = """Supported pixel formats:([^\n]*)""".toRegex()
    private val implementationSupportedSampleRatesRegex = """Supported sample rates:([^\n]*)""".toRegex()
    private val implementationSupportedSampleFormatsRegex = """Supported sample formats:([^\n]*)""".toRegex()
    private val implementationSupportedChannelLayoutsRegex = """Supported channel layouts:([^\n]*)""".toRegex()

    private fun probeImplementation(implementation: ImplementationCapabilities): ImplementationCapabilities? {
        val kind = when (implementation.direction) {
            CodecDirection.DECODE -> "decoder"
            CodecDirection.ENCODE -> "encoder"
        }

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-h", "$kind=${implementation.id}",
        )

        if (result.error) {
            return null
        }

        val generalCapabilities =
            parseSpaceSeparatedSequence(implementationGeneralCapabilitiesRegex, result.stdout)
        val supportedHardwareDevices =
            parseSpaceSeparatedSequence(implementationSupportedHardwareDevicesRegex, result.stdout)
        val supportedPixelFormats =
            parseSpaceSeparatedSequence(implementationSupportedPixelFormatsRegex, result.stdout)
        val supportedSampleRates =
            parseSpaceSeparatedSequence(implementationSupportedSampleRatesRegex, result.stdout)
        val supportedSampleFormats =
            parseSpaceSeparatedSequence(implementationSupportedSampleFormatsRegex, result.stdout)
        val supportedChannelLayouts =
            parseSpaceSeparatedSequence(implementationSupportedChannelLayoutsRegex, result.stdout)

        val general = generalCapabilities.toSet()

        return implementation.copy(
            kind = when {
                "hardware" in general -> ImplementationKind.HARDWARE
                "hybrid" in general -> ImplementationKind.HYBRID
                else -> ImplementationKind.SOFTWARE
            },
            generalCapabilities = general,
            supportedHardwareDevices = supportedHardwareDevices.map(::DeviceId).toSet(),
            supportedPixelFormats = supportedPixelFormats.map(::PixelFormat).toSet(),
            supportedSampleRates = supportedSampleRates.map(String::toLong).toSet(),
            supportedSampleFormats = supportedSampleFormats.toSet(),
            supportedChannelLayouts = supportedChannelLayouts.toSet(),
        )
    }

    private val coderLineRegex = """^\s*([VASFXBD.]{6})\s+(\S+)\s+(.*)$""".toRegex()

    private fun parseImplementations(text: String, direction: CodecDirection): List<ImplementationCapabilities> {
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
                ImplementationKind.SOFTWARE,
            )
        }

        return result
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
