package dev.scriptor

import dev.scriptor.model.ffmpeg.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.BufferedReader
import java.util.logging.Logger

class Probe(
    private val log: Logger,
    private val ffmpeg: String = "ffmpeg",
    private val device: String? = null,
) {
    operator fun invoke(database: Database) = probe(database)

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

    private fun probe(database: Database) {
        context(database) {
            val devices = probeDevices()

            probeDeviceToDevice(devices)

            probeFormats()
            probeFilters()

            val decodersMap = mutableMapOf<CodecId, Set<ImplementationId>>()
            val encodersMap = mutableMapOf<CodecId, Set<ImplementationId>>()

            probeCodecs(decodersMap, encodersMap)

            val decodersCodecMap = decodersMap.entries.flatMap { (key, value) -> value.map { it to key } }.toMap()
            val encodersCodecMap = encodersMap.entries.flatMap { (key, value) -> value.map { it to key } }.toMap()

            val decoders = probeDecoders(decodersCodecMap)
            val encoders = probeEncoders(encodersCodecMap)

            probeImplementations(
                devices.map { it.id.value }.toSet(),
                decoders + encoders,
            )
        }
    }

    context(database: Database)
    private fun probeDevices(): List<Device> {
        log.fine("probe devices")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-init_hw_device", "list",
        )

        if (result.error) {
            log.fine("failed to probe device types:\n${result.stderr}")
            return emptyList()
        }

        val devices = result.stdout
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .drop(1)
            .map(::DeviceId)
            .toSet()

        val existing = transaction(database) {
            DeviceTable
                .select(DeviceTable.id)
                .map { it[DeviceTable.id].value }
                .toSet()
        }

        return devices
            .filter { it !in existing }
            .mapNotNull { probeDevice(it) }
    }

    context(database: Database)
    private fun probeDevice(
        id: DeviceId,
    ): Device? {
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

        return transaction(database) { Device.new(id) {} }
    }

    context(database: Database)
    private fun probeFormats() {
        log.fine("probe formats")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-pix_fmts",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe formats:\n$message")
            return
        }

        parseFormats(result.stdout)
    }

    context(database: Database)
    private fun probeFilters() {
        log.fine("probe filters")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-filters",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe filters:\n$message")
            return
        }

        parseFilters(result.stdout)
    }

    context(database: Database)
    private fun probeCodecs(
        decodersMap: MutableMap<CodecId, Set<ImplementationId>>,
        encodersMap: MutableMap<CodecId, Set<ImplementationId>>,
    ) {
        log.fine("probe codecs")

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-codecs",
        )

        if (result.error) {
            val message = result.stderr.ifBlank(result::stdout)
            log.fine("failed to probe encoders:\n$message")
            return
        }

        parseCodecs(
            result.stdout,
            decodersMap,
            encodersMap,
        )
    }

    context(database: Database)
    private fun probeDecoders(codecMap: Map<ImplementationId, CodecId>): List<Implementation> {
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

        return parseImplementations(result.stdout, ImplementationDirection.DECODE, codecMap)
    }

    context(database: Database)
    private fun probeEncoders(codecMap: Map<ImplementationId, CodecId>): List<Implementation> {
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

        return parseImplementations(result.stdout, ImplementationDirection.ENCODE, codecMap)
    }

    context(database: Database)
    private fun probeImplementations(
        devices: Set<DeviceId>,
        implementations: List<Implementation>,
    ) {
        log.fine("probe implementations (${implementations.size})")

        for (implementation in implementations) {
            probeImplementation(devices, implementation)
        }
    }

    context(database: Database)
    private fun probeDeviceToDevice(devices: List<Device>) {
        val existing = transaction(database) {
            DeviceToDeviceTable
                .select(DeviceToDeviceTable.src, DeviceToDeviceTable.dst)
                .map { it[DeviceToDeviceTable.src].value to it[DeviceToDeviceTable.dst].value }
                .toSet()
        }

        val combinations = mutableListOf<Pair<DeviceId, DeviceId>>()
        for (src in devices) {
            for (dst in devices) {
                val key = src.id.value to dst.id.value
                if (key in existing) continue
                combinations.add(key)
            }
        }

        for ((src, dst) in combinations) {
            if (src == dst) {
                transaction(database) {
                    DeviceToDevice.new {
                        this.src = Device[src]
                        this.dst = Device[dst]
                        this.derivable = true
                        this.direct = true
                        this.mapping = true
                    }
                }
                continue
            }

            val data = probeDeviceToDevice(src, dst) ?: continue

            transaction(database) {
                DeviceToDevice.new {
                    this.src = Device[src]
                    this.dst = Device[dst]
                    this.derivable = data.derivable
                    this.direct = data.direct
                    this.mapping = data.mapping
                }
            }
        }
    }

    private data class DeviceToDeviceData(
        val derivable: Boolean,
        val direct: Boolean,
        val mapping: Boolean,
    )

    private fun probeDeviceToDevice(src: DeviceId, dst: DeviceId): DeviceToDeviceData? {
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
            return DeviceToDeviceData(
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
            return DeviceToDeviceData(
                derivable = true,
                direct = false,
                mapping = true,
            )
        }

        return DeviceToDeviceData(
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

    context(database: Database)
    private fun parseCodecs(
        text: String,
        decodersMap: MutableMap<CodecId, Set<ImplementationId>>,
        encodersMap: MutableMap<CodecId, Set<ImplementationId>>,
    ) {
        val existing = transaction(database) {
            CodecTable
                .select(CodecTable.id)
                .map { it[CodecTable.id].value }
                .toSet()
        }

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
            if (id in existing) continue

            val decoders = parseSpaceSeparatedSequence(codecDecodersRegex, description)
                .map(::ImplementationId)
                .toSet()
            val encoders = parseSpaceSeparatedSequence(codecEncodersRegex, description)
                .map(::ImplementationId)
                .toSet()

            decodersMap[id] = decoders
            encodersMap[id] = encoders

            transaction(database) {
                Codec.new(id) {
                    this.type = type
                    this.supportsDecoding = flags[0] == 'D'
                    this.supportsEncoding = flags[1] == 'E'
                    this.intraFrameOnly = flags[3] == 'I'
                    this.lossyCompression = flags[4] == 'L'
                    this.losslessCompression = flags[5] == 'S'
                }
            }
        }
    }

    private val implementationGeneralCapabilitiesRegex = """General capabilities:([^\n]*)""".toRegex()
    private val implementationSupportedHardwareDevicesRegex = """Supported hardware devices:([^\n]*)""".toRegex()
    private val implementationSupportedPixelFormatsRegex = """Supported pixel formats:([^\n]*)""".toRegex()
    private val implementationSupportedSampleRatesRegex = """Supported sample rates:([^\n]*)""".toRegex()
    private val implementationSupportedSampleFormatsRegex = """Supported sample formats:([^\n]*)""".toRegex()
    private val implementationSupportedChannelLayoutsRegex = """Supported channel layouts:([^\n]*)""".toRegex()

    context(database: Database)
    private fun probeImplementation(
        devices: Set<DeviceId>,
        implementation: Implementation,
    ): Boolean {
        val kind = when (implementation.direction) {
            ImplementationDirection.DECODE -> "decoder"
            ImplementationDirection.ENCODE -> "encoder"
        }

        val result = command(
            "-hide_banner",
            "-loglevel", "error",
            "-h", "$kind=${implementation.id}",
        )

        if (result.error) {
            return false
        }

        val generalCapabilities =
            parseSpaceSeparatedSequence(implementationGeneralCapabilitiesRegex, result.stdout)
                .toSet()
        val supportedHardwareDevices =
            parseSpaceSeparatedSequence(implementationSupportedHardwareDevicesRegex, result.stdout)
                .map { DeviceId(it) }
                .filter { it in devices }
                .toSet()
        val supportedPixelFormats =
            parseSpaceSeparatedSequence(implementationSupportedPixelFormatsRegex, result.stdout)
                .map { FormatId(it) }
                .toSet()
        val supportedSampleRates =
            parseSpaceSeparatedSequence(implementationSupportedSampleRatesRegex, result.stdout)
                .map { it.toLong() }
                .toSet()
        val supportedSampleFormats =
            parseSpaceSeparatedSequence(implementationSupportedSampleFormatsRegex, result.stdout)
                .toSet()
        val supportedChannelLayouts =
            parseSpaceSeparatedSequence(implementationSupportedChannelLayoutsRegex, result.stdout)
                .toSet()

        transaction(database) {
            implementation.kind = when {
                "hardware" in generalCapabilities -> ImplementationKind.HARDWARE
                "hybrid" in generalCapabilities -> ImplementationKind.HYBRID
                else -> ImplementationKind.SOFTWARE
            }

            implementation.generalCapabilities = generalCapabilities
            implementation.supportedSampleRates = supportedSampleRates
            implementation.supportedSampleFormats = supportedSampleFormats
            implementation.supportedChannelLayouts = supportedChannelLayouts

            supportedHardwareDevices.forEach { device ->
                ImplementationDeviceTable.insert {
                    it[ImplementationDeviceTable.implementation] = implementation.id
                    it[ImplementationDeviceTable.device] = device
                }
            }

            supportedPixelFormats.forEach { format ->
                ImplementationFormatTable.insert {
                    it[ImplementationFormatTable.implementation] = implementation.id
                    it[ImplementationFormatTable.format] = format
                }
            }
        }

        return true
    }

    private val implementationLineRegex = """^\s*([VASFXBD.]{6})\s+(\S+)\s+(.*)$""".toRegex()

    context(database: Database)
    private fun parseImplementations(
        text: String,
        direction: ImplementationDirection,
        codecMap: Map<ImplementationId, CodecId>,
    ): List<Implementation> {
        val existing = transaction(database) {
            ImplementationTable
                .select(ImplementationTable.id)
                .map { it[ImplementationTable.id].value }
                .toSet()
        }

        val result = mutableListOf<Implementation>()

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = implementationLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]

            val id = ImplementationId(name)
            if (id in existing) continue

            val codecId = codecMap[id]

            val codecType = when (val c = flags[0]) {
                'V' -> CodecType.VIDEO
                'A' -> CodecType.AUDIO
                'S' -> CodecType.SUBTITLE
                else -> error("unexpected codec type '$c'")
            }

            val frameLevelMultithreading = flags[1] == 'F'
            val sliceLevelMultithreading = flags[2] == 'S'
            val experimental = flags[3] == 'X'
            val supportDrawHorizontalBand = flags[4] == 'B'
            val supportDirectRendering = flags[5] == 'D'

            result += transaction(database) {
                val codec = if (codecId != null) Codec[codecId] else null

                if (codec != null && codec.type != codecType) {
                    error("codec type mismatch")
                }

                Implementation.new(id) {
                    this.codec = codec
                    this.direction = direction
                    this.frameLevelMultithreading = frameLevelMultithreading
                    this.sliceLevelMultithreading = sliceLevelMultithreading
                    this.experimental = experimental
                    this.supportDrawHorizontalBand = supportDrawHorizontalBand
                    this.supportDirectRendering = supportDirectRendering
                    this.kind = ImplementationKind.SOFTWARE
                }
            }
        }

        return result
    }

    private val formatLineRegex = """^\s*([IOHPB.]{5})\s+(\S+)\s+(\d+)\s+(\d+)\s+(\d(?:-\d)*)$""".toRegex()

    context(database: Database)
    private fun parseFormats(text: String) {
        val existing = transaction(database) {
            FormatTable
                .select(FormatTable.id)
                .map { it[FormatTable.id].value }
                .toSet()
        }

        for (line in text.lines().dropWhile { !it.trim().startsWith('-') }) {
            val match = formatLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]
            val nbComponents = match.groupValues[3].toInt()
            val bitsPerPixel = match.groupValues[4].toInt()
            val bitDepths = match.groupValues[5].split("-", limit = nbComponents).map { it.toInt() }

            val id = FormatId(name)
            if (id in existing) continue

            val input = flags[0] == 'I'
            val output = flags[1] == 'O'
            val hardware = flags[2] == 'H'
            val paletted = flags[3] == 'P'
            val bitstream = flags[4] == 'B'

            transaction(database) {
                Format.new(id) {
                    this.components = bitDepths
                    this.bitsPerPixel = bitsPerPixel
                    this.input = input
                    this.output = output
                    this.hardware = hardware
                    this.paletted = paletted
                    this.bitstream = bitstream
                }
            }
        }
    }

    private val filterLineRegex = """^\s*([TS.]{2,3})\s+(\S+)\s+([AVN|\->]+)\s+(.+)$""".toRegex()

    context(database: Database)
    private fun parseFilters(text: String) {
        val existing = transaction(database) {
            FilterTable
                .select(FilterTable.id)
                .map { it[FilterTable.id].value }
                .toSet()
        }

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = filterLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]
            val transform = match.groupValues[3]

            val id = FilterId(name)
            if (id in existing) continue

            transaction(database) {
                Filter.new(id) {
                    this.transform = transform
                    this.timelineSupport = flags[0] == 'T'
                    this.sliceThreading = flags[1] == 'S'
                }
            }
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
