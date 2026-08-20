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

        log.fine("probe encoders")

        val encoders = probeEncoders()

        log.fine("probe decoders")

        val decoders = probeDecoders()

        log.fine("probe filters")

        val filters = probeFilters()

        log.fine("probe devices")

        val devices = deviceTypes.mapNotNull { type ->
            probeDevice(
                type,
                encoders,
                decoders,
                filters,
            )
        }

        log.fine("probe interop")

        val interop = probeInterop(devices)

        return Capabilities(
            devices,
            encoders,
            decoders,
            filters,
            interop,
        )
    }

    private fun probeDeviceTypes(): List<String> {
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
            .toList()
    }

    private fun probeEncoders(): List<CodecCapabilities> {
        val encoders = command(
            "-hide_banner",
            "-loglevel", "error",
            "-encoders",
        )

        if (encoders.error) {
            val message = encoders.stderr.ifBlank(encoders::stdout)
            error("failed to probe encoders:\n$message")
        }

        return parseCodecs(encoders.stdout, CodecDirection.ENCODE)
    }

    private fun probeDecoders(): List<CodecCapabilities> {
        val decoders = command(
            "-hide_banner",
            "-loglevel", "error",
            "-decoders",
        )

        if (decoders.error) {
            val message = decoders.stderr.ifBlank(decoders::stdout)
            error("failed to probe decoders:\n$message")
        }

        return parseCodecs(decoders.stdout, CodecDirection.DECODE)
    }

    private fun probeFilters(): List<FilterCapabilities> {
        val filters = command(
            "-hide_banner",
            "-loglevel", "error",
            "-filters",
        )

        if (filters.error) {
            val message = filters.stderr.ifBlank(filters::stdout)
            error("failed to probe filters:\n$message")
        }

        return parseFilters(filters.stdout)
    }

    private fun probeDevice(
        type: String,
        encoders: List<CodecCapabilities>,
        decoders: List<CodecCapabilities>,
        filters: List<FilterCapabilities>,
    ): DeviceCapabilities? {
        val init = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$type=probe"
        )

        if (init.error) {
            return null
        }

        val encoderSet = encoders
            .filter { codecBelongsToDevice(it.name, type) }
            .map { it.name }
            .toSet()

        val decoderSet = decoders
            .filter { codecBelongsToDevice(it.name, type) }
            .map { it.name }
            .toSet()

        val filterSet = filters
            .filter { filterBelongsToDevice(it.name, type) }
            .map { it.name }
            .toSet()

        return DeviceCapabilities(
            type,
            encoderSet,
            decoderSet,
            filterSet,
        )
    }

    private fun probeInterop(devices: List<DeviceCapabilities>): List<InteropCapabilities> {
        val result = mutableListOf<InteropCapabilities>()

        for (src in devices) {
            for (dst in devices) {
                if (src == dst) {
                    result += InteropCapabilities(
                        src = src.type,
                        dst = dst.type,
                        derivable = true,
                        direct = true,
                        mapping = true,
                    )
                    continue
                }

                result += probeDeviceInterop(src.type, dst.type) ?: continue
            }
        }

        return result
    }

    private fun probeDeviceInterop(src: String, dst: String): InteropCapabilities? {
        log.finer("probe device interop: $src <---> $dst")

        val derive = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
        )

        if (derive.error) {
            return null
        }

        val direct = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "color=size=64x64:rate=1:color=black",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=$dst:direct=1,hwdownload,format=nv12",
            "-f", "null",
            "-",
        )

        if (direct.success) {
            return InteropCapabilities(
                src = src,
                dst = dst,
                derivable = true,
                direct = true,
                mapping = true,
            )
        }

        val mapping = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "$src=src",
            "-init_hw_device", "$dst=dst@src",
            "-filter_hw_device", "src",
            "-f", "lavfi",
            "-i", "color=size=64x64:rate=1:color=black",
            "-frames:v", "1",
            "-vf", "format=nv12,hwupload,hwmap=derive_device=$dst,hwdownload,format=nv12",
            "-f", "null",
            "-",
        )

        if (mapping.success) {
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

    private val codecGeneralCapabilitiesRegex = """General capabilities:\s*(.+)""".toRegex()
    private val codecThreadingCapabilitiesRegex = """Threading capabilities:\s*(.+)""".toRegex()
    private val codecSupportedPixelFormatsRegex = """Supported pixel formats:\s*(.+)""".toRegex()
    private val codecSupportedSampleRatesRegex = """Supported sample rates:\s*(.+)""".toRegex()
    private val codecSupportedSampleFormatsRegex = """Supported sample formats:\s*(.+)""".toRegex()
    private val codecSupportedChannelLayoutsRegex = """Supported channel layouts:\s*(.+)""".toRegex()

    private fun parseCodecCapabilities(regex: Regex, text: String): Set<String> {
        val match = regex.find(text)

        return if (match != null) {
            match.groupValues[1]
                .split("\\s+".toRegex())
                .filter(String::isNotBlank)
                .toSet()
        } else emptySet()
    }

    private fun probeCodecCapabilities(codec: CodecCapabilities): CodecCapabilities {
        val kind = when (codec.direction) {
            CodecDirection.ENCODE -> "encoder"
            CodecDirection.DECODE -> "decoder"
        }

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-h", "$kind=${codec.name}",
        )

        val generalCapabilities = parseCodecCapabilities(codecGeneralCapabilitiesRegex, result.stdout)
        val threadingCapabilities = parseCodecCapabilities(codecThreadingCapabilitiesRegex, result.stdout)
        val supportedPixelFormats = parseCodecCapabilities(codecSupportedPixelFormatsRegex, result.stdout)
        val supportedSampleRates = parseCodecCapabilities(codecSupportedSampleRatesRegex, result.stdout)
        val supportedSampleFormats = parseCodecCapabilities(codecSupportedSampleFormatsRegex, result.stdout)
        val supportedChannelLayouts = parseCodecCapabilities(codecSupportedChannelLayoutsRegex, result.stdout)

        return codec.copy(
            generalCapabilities = generalCapabilities,
            threadingCapabilities = threadingCapabilities,
            pixelFormats = supportedPixelFormats,
            sampleRates = supportedSampleRates.map(String::toLong).toSet(),
            sampleFormats = supportedSampleFormats,
            channelLayouts = supportedChannelLayouts,
        )
    }

    private fun parseConfiguration(text: String): List<String> {
        val line = text
            .lineSequence()
            .firstOrNull { it.trimStart().startsWith("configuration:") }

        return line
            ?.substringAfter("configuration:")
            ?.trim()
            ?.split("\\s+".toRegex())
            ?.filter(String::isNotEmpty)
            ?: emptyList()
    }

    private val codecLineRegex = """^\s*([VASFXBD.]{6})\s+(\S+)\s+(.*)$""".toRegex()

    private fun parseCodecs(text: String, direction: CodecDirection): List<CodecCapabilities> {
        val result = mutableListOf<CodecCapabilities>()

        for (line in text.lineSequence().dropWhile { it.trim() != "------" }) {
            val match = codecLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]
            val description = match.groupValues[3]

            val type = when (flags[0]) {
                'V' -> CodecType.VIDEO
                'A' -> CodecType.AUDIO
                'S' -> CodecType.SUBTITLE
                else -> null
            } ?: continue

            result += CodecCapabilities(
                name,
                description,
                type,
                direction,
                flags[1] == 'F',
                flags[2] == 'S',
                flags[3] == 'X',
                flags[4] == 'B',
                flags[5] == 'D',
            )
        }

        return result.map(::probeCodecCapabilities)
    }

    private val filterLineRegex = """^\s*([TS.]{2,3})\s+(\S+)\s+([AVN|\->]+)\s+(.+)$""".toRegex()

    private fun parseFilters(text: String): List<FilterCapabilities> {
        val result = mutableListOf<FilterCapabilities>()

        for (line in text.lineSequence().dropWhile { it.trim() != "------" }) {
            val match = filterLineRegex.matchEntire(line) ?: continue

            val type = match.groupValues[1]
            val name = match.groupValues[2]
            val transform = match.groupValues[3]
            val description = match.groupValues[4]

            result += FilterCapabilities(
                type,
                name,
                transform,
                description,
            )
        }

        return result
    }

    private fun codecBelongsToDevice(codec: String, device: String): Boolean {
        val c = codec.lowercase()
        val d = device.lowercase()

        return when (d) {
            "cuda" -> "cuda" in c || "cuvid" in c || "nvenc" in c
            "vaapi" -> "vaapi" in c
            "qsv" -> "qsv" in c
            "vulkan" -> "vulkan" in c
            "vdpau" -> "vdpau" in c
            "d3d11va" -> "d3d11" in c
            "d3d12va" -> "d3d12" in c
            "dxva2" -> "dxva2" in c
            "videotoolbox" -> "videotoolbox" in c
            "amf" -> "amf" in c
            else -> false
        }
    }

    private fun filterBelongsToDevice(filter: String, device: String): Boolean {
        val f = filter.lowercase()
        val d = device.lowercase()

        return when (d) {
            "cuda" -> "cuda" in f
            "vaapi" -> "vaapi" in f
            "qsv" -> "qsv" in f
            "vulkan" -> "vulkan" in f
            "opencl" -> "opencl" in f
            "vdpau" -> "vdpau" in f
            "amf" -> "amf" in f
            "d3d11va" -> "d3d11" in f
            "d3d12va" -> "d3d12" in f
            else -> false
        }
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
