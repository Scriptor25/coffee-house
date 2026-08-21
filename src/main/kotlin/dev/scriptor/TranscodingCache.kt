package dev.scriptor

import dev.scriptor.backend.VideoBackend
import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.ffmpeg.Capabilities
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.DeviceId
import dev.scriptor.model.ffmpeg.FilterId
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.VideoTrack
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class TranscodingCache(
    private val ffmpeg: String,
    private val base: Path,
    private val capabilities: Capabilities,
    private val requirements: TranscodingRequirements,
    private val allowed: Set<String> = setOf("2160p", "1440p", "1080p", "720p", "480p", "360p", "144p"),
) {
    private val definedVariants: List<Variant> = listOf(
        ScaleVariant("2160p", 3840, 2160, 16_000_000L, Profile.HIGH),
        ScaleVariant("1440p", 2560, 1440, 8_000_000L, Profile.HIGH),
        ScaleVariant("1080p", 1920, 1080, 6_000_000L, Profile.MEDIUM),
        ScaleVariant("720p", 1280, 720, 3_000_000L, Profile.MEDIUM),
        ScaleVariant("480p", 854, 480, 1_500_000L, Profile.LOW),
        ScaleVariant("360p", 640, 360, 750_000L, Profile.LOW),
        ScaleVariant("144p", 256, 144, 300_000L, Profile.POTATO),
    )

    private val jobs: MutableMap<Uuid, TranscodingJob> = ConcurrentHashMap()

    fun variants(item: VideoTrack): List<Variant> {
        val sourceWidth = item.width - (item.width % 2)
        val sourceHeight = item.height - (item.height % 2)

        val result = mutableListOf(
            if (requirements.enable && sourceWidth != item.width || sourceHeight != item.height)
                ScaleVariant(
                    "original",
                    sourceWidth,
                    sourceHeight,
                    item.bitRate,
                    Profile.ARCHIVAL,
                )
            else
                OriginalVariant()
        )

        if (requirements.enable) {
            for (variant in definedVariants) {
                if (variant.name !in allowed) continue
                if (variant !is ScaleVariant) continue

                // no upscaling
                if (variant.width > item.width || variant.height > item.height) continue
                if (variant.width == item.width && variant.height == item.height) continue

                val aspect = item.width.toDouble() / item.height.toDouble()
                val width = (variant.height * aspect).toInt()
                val height = variant.height

                result += ScaleVariant(
                    variant.name,
                    width - (width % 2),
                    height - (height % 2),
                    variant.bitrate,
                    variant.profile,
                )
            }
        }

        return result
    }

    private fun createBackend(device: DeviceId?, input: CodecId, output: CodecId): VideoBackend {
        val decoder = capabilities.getDecoders(input, device).first()
        val encoder = capabilities.getEncoders(output, device).first()

        val videoEncoder = VideoEncoder.find(encoder) ?: error("encoder '$encoder' not implemented")

        // TODO: find scale filter for device
        val scale = FilterId("scale")

        return when (device) {
            null -> VideoBackend(
                device,
                { emptyList() },
                { emptyList() },
                { listOf("-c:v", "$decoder") },
                { width, height -> listOf("$scale=w=$width:h=$height") },
                videoEncoder,
            )

            else -> VideoBackend(
                device,
                { listOf("hwupload") },
                { listOf("hwdownload") },
                { listOf("-hwaccel", "$device", "-c:v", "$decoder") },
                { width, height -> listOf("$scale=w=$width:h=$height") },
                videoEncoder,
            )
        }
    }

    context(database: Database)
    fun job(item: Media): TranscodingJob = jobs.computeIfAbsent(item.id.value) {
        transaction(database) {

            val video = item.video.first { it.index == 0 }

            val input = CodecId(video.codec)
            val output = requirements.video

            val decodeDevices = capabilities.getDevicesForDecoding(input)
            val encodeDevices = capabilities.getDevicesForEncoding(output)

            val transcodeDevice = decodeDevices.filter { it in encodeDevices }.toSet().firstOrNull()

            val pipeline = if (transcodeDevice == null) {
                // TODO: find most suitable device for decoding/encoding

                val decodeDevice = decodeDevices.firstOrNull()
                val encodeDevice = encodeDevices.firstOrNull()

                val decodeBackend = createBackend(decodeDevice, input, output)
                val encodeBackend = createBackend(encodeDevice, input, output)

                // TODO: find separate device and backend for splitting/scaling

                Pipeline(
                    capabilities,
                    decodeBackend,
                    encodeBackend,
                    encodeBackend,
                    encodeBackend,
                )
            } else {
                val backend = createBackend(transcodeDevice, input, output)

                Pipeline(capabilities, backend)
            }

            TranscodingJob(
                ffmpeg,
                item,
                base.resolve(item.id.value.toHexDashString()),
                variants(video),
                requirements.enable,
                requirements.device,
                pipeline,
            )
        }
    }
}
