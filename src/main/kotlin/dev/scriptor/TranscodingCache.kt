package dev.scriptor

import dev.scriptor.backend.VideoBackend
import dev.scriptor.decoder.video.VideoDecoder
import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.ffmpeg.Capabilities
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.DeviceBackend
import dev.scriptor.model.ffmpeg.DeviceId
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
        val decoders = capabilities.getDecoders(input, device)
        val encoders = capabilities.getEncoders(output, device)

        val decoder = decoders.firstOrNull()
        val encoder = encoders.firstOrNull()

        val videoDecoder =
            if (decoder != null) VideoDecoder.find(decoder)
                ?: error("decoder '$decoder' not implemented")
            else VideoDecoder.Null
        val videoEncoder =
            if (encoder != null) VideoEncoder.find(encoder)
                ?: error("encoder '$encoder' not implemented")
            else VideoEncoder.Null

        return when (device) {
            null -> object : VideoBackend {
                override val device = device

                override val decoder = videoDecoder
                override val encoder = videoEncoder

                override fun upload(): List<String> = emptyList()
                override fun download(): List<String> = emptyList()

                override fun scale(width: Int, height: Int): List<String> = listOf("scale=w=$width:h=$height")
            }

            else -> {
                val backend = DeviceBackend.entries.find { it.device == device }
                    ?: error("device '$device' not implemented")

                val format = backend.format
                val scale = backend.scale

                { // software decode
                    listOf("-c:v", "$decoder")
                }

                { // hardware decode
                    listOf(
                        "-hwaccel", "$device",
                        "-hwaccel_output_format", "$format",
                        "-c:v", "$decoder",
                    )
                }

                object : VideoBackend {
                    override val device = device

                    override val decoder = videoDecoder
                    override val encoder = videoEncoder

                    override fun upload(): List<String> = listOf("format=nv12", "hwupload")
                    override fun download(): List<String> = listOf("hwdownload", "format=nv12")

                    override fun scale(width: Int, height: Int): List<String> = listOf("$scale=w=$width:h=$height")
                }
            }
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

            val transcodeDevice = decodeDevices
                .filter(encodeDevices::contains)
                .toSet()
                .firstOrNull()

            val pipeline = if (transcodeDevice == null) {
                // TODO: find most suitable device for decoding/encoding

                val decodeDevice = decodeDevices.firstOrNull()
                val encodeDevice = encodeDevices.firstOrNull()

                val decodeBackend = createBackend(decodeDevice, input, output)
                val encodeBackend =
                    if (decodeDevice == encodeDevice) decodeBackend
                    else createBackend(encodeDevice, input, output)

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
