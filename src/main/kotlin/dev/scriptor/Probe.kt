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

            val decodersMap = mutableMapOf<CodecId, Set<ImplementationId>>()
            val encodersMap = mutableMapOf<CodecId, Set<ImplementationId>>()

            probeCodecs(decodersMap, encodersMap)
            probeFilters()

            val decodersCodecMap = decodersMap.entries.flatMap { (key, value) -> value.map { it to key } }.toMap()
            val encodersCodecMap = encodersMap.entries.flatMap { (key, value) -> value.map { it to key } }.toMap()

            val decoders = probeDecoders(decodersCodecMap)
            val encoders = probeEncoders(encodersCodecMap)

            probeDeviceToDevice(devices)
            probeImplementations(
                devices.map { it.id.value }.toSet(),
                decoders + encoders,
            )
        }
    }

    context(database: Database)
    private fun probeDevices(): List<DeviceCapabilities> {
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
            DeviceCapabilitiesTable
                .select(DeviceCapabilitiesTable.id)
                .map { it[DeviceCapabilitiesTable.id].value }
                .toSet()
        }

        return devices
            .filter { it !in existing }
            .mapNotNull { probeDevice(it) }
    }

    context(database: Database)
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

        return transaction(database) { DeviceCapabilities.new(id) {} }
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
    private fun probeDecoders(codecMap: Map<ImplementationId, CodecId>): List<ImplementationCapabilities> {
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

        return parseImplementations(result.stdout, CodecDirection.DECODE, codecMap)
    }

    context(database: Database)
    private fun probeEncoders(codecMap: Map<ImplementationId, CodecId>): List<ImplementationCapabilities> {
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

        return parseImplementations(result.stdout, CodecDirection.ENCODE, codecMap)
    }

    context(database: Database)
    private fun probeImplementations(
        devices: Set<DeviceId>,
        implementations: List<ImplementationCapabilities>,
    ) {
        log.fine("probe implementations (${implementations.size})")

        for (implementation in implementations) {
            probeImplementation(devices, implementation)
        }
    }

    context(database: Database)
    private fun probeDeviceToDevice(devices: List<DeviceCapabilities>) {
        val existing = transaction(database) {
            DeviceToDeviceCapabilitiesTable
                .select(DeviceToDeviceCapabilitiesTable.src, DeviceToDeviceCapabilitiesTable.dst)
                .map { it[DeviceToDeviceCapabilitiesTable.src].value to it[DeviceToDeviceCapabilitiesTable.dst].value }
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
                    DeviceToDeviceCapabilities.new {
                        this.src = DeviceCapabilities[src]
                        this.dst = DeviceCapabilities[dst]
                        this.derivable = true
                        this.direct = true
                        this.mapping = true
                    }
                }
                continue
            }

            val data = probeDeviceToDevice(src, dst) ?: continue

            transaction(database) {
                DeviceToDeviceCapabilities.new {
                    this.src = DeviceCapabilities[src]
                    this.dst = DeviceCapabilities[dst]
                    this.derivable = data.derivable
                    this.direct = data.direct
                    this.mapping = data.mapping
                }
            }
        }
    }

    private data class DeviceToDeviceCapabilitiesData(
        val derivable: Boolean,
        val direct: Boolean,
        val mapping: Boolean,
    )

    private fun probeDeviceToDevice(src: DeviceId, dst: DeviceId): DeviceToDeviceCapabilitiesData? {
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
            return DeviceToDeviceCapabilitiesData(
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
            return DeviceToDeviceCapabilitiesData(
                derivable = true,
                direct = false,
                mapping = true,
            )
        }

        return DeviceToDeviceCapabilitiesData(
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
            CodecCapabilitiesTable
                .select(CodecCapabilitiesTable.id)
                .map { it[CodecCapabilitiesTable.id].value }
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
                CodecCapabilities.new(id) {
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
        implementation: ImplementationCapabilities,
    ): Boolean {
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
                .map { PixelFormat(it) }
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

            implementation.generalCapabilities = generalCapabilities.joinToString(";")
            implementation.supportedPixelFormats = supportedPixelFormats.joinToString(";")
            implementation.supportedSampleRates = supportedSampleRates.joinToString(";")
            implementation.supportedSampleFormats = supportedSampleFormats.joinToString(";")
            implementation.supportedChannelLayouts = supportedChannelLayouts.joinToString(";")

            supportedHardwareDevices.forEach { device ->
                ImplementationDeviceTable.insert {
                    it[ImplementationDeviceTable.implementation] = implementation.id
                    it[ImplementationDeviceTable.device] = device
                }
            }
        }

        return true
    }

    private val coderLineRegex = """^\s*([VASFXBD.]{6})\s+(\S+)\s+(.*)$""".toRegex()

    context(database: Database)
    private fun parseImplementations(
        text: String,
        direction: CodecDirection,
        codecMap: Map<ImplementationId, CodecId>,
    ): List<ImplementationCapabilities> {
        val existing = transaction(database) {
            ImplementationCapabilitiesTable
                .select(ImplementationCapabilitiesTable.id)
                .map { it[ImplementationCapabilitiesTable.id].value }
                .toSet()
        }

        val result = mutableListOf<ImplementationCapabilities>()

        for (line in text.lineSequence().dropWhile { !it.trim().startsWith('-') }) {
            val match = coderLineRegex.matchEntire(line) ?: continue

            val flags = match.groupValues[1]
            val name = match.groupValues[2]

            val id = ImplementationId(name)
            if (id in existing) continue

            val codec = codecMap[id]

            result += transaction(database) {
                ImplementationCapabilities.new(id) {
                    this.codec = if (codec == null) null else CodecCapabilities[codec]
                    this.direction = direction
                    this.frameLevelMultithreading = flags[1] == 'F'
                    this.sliceLevelMultithreading = flags[2] == 'S'
                    this.experimental = flags[3] == 'X'
                    this.supportDrawHorizontalBand = flags[4] == 'B'
                    this.supportDirectRendering = flags[5] == 'D'
                    this.kind = ImplementationKind.SOFTWARE
                }
            }
        }

        return result
    }

    private val filterLineRegex = """^\s*([TS.]{2,3})\s+(\S+)\s+([AVN|\->]+)\s+(.+)$""".toRegex()

    context(database: Database)
    private fun parseFilters(text: String) {
        val existing = transaction(database) {
            FilterCapabilitiesTable
                .select(FilterCapabilitiesTable.id)
                .map { it[FilterCapabilitiesTable.id].value }
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
                FilterCapabilities.new(id) {
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
