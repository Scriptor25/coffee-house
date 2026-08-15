package dev.scriptor

import dev.scriptor.backend.*
import dev.scriptor.codec.AudioCodec
import dev.scriptor.codec.SubtitleCodec
import dev.scriptor.codec.VideoCodec
import dev.scriptor.model.Media
import dev.scriptor.model.VideoTrack
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class HlsCache(
    private val base: Path,
    private val transcoding: Boolean,
    private val capabilities: TranscodingCapabilities,
    private val preferredVideoCodec: VideoCodec,
    private val preferredAudioCodec: AudioCodec,
    private val preferredSubtitleCodec: SubtitleCodec,
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
            if (transcoding && (sourceWidth != item.width || sourceHeight != item.height))
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

        if (transcoding) {
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

    fun supportsEncoding(name: String, codec: VideoCodec): Boolean {
        return capabilities.encoders
            .any { it.type == CodecType.VIDEO && "${codec.name.lowercase()}_$name" in it.name }
    }

    fun selectBackend(codec: VideoCodec): VideoBackend = when {
        "cuda" in capabilities.devices && supportsEncoding("nvenc", codec) -> NvidiaVideoBackend
        "qsv" in capabilities.devices && supportsEncoding("qsv", codec) -> IntelVideoBackend
        "amf" in capabilities.devices && supportsEncoding("amf", codec) -> AmdVideoBackend
        "vaapi" in capabilities.devices && supportsEncoding("vaapi", codec) -> VaapiVideoBackend
        "vulkan" in capabilities.devices && supportsEncoding("vulkan", codec) -> VulkanVideoBackend
        else -> SoftwareVideoBackend
    }

    context(database: Database)
    fun job(item: Media): TranscodingJob =
        jobs.computeIfAbsent(item.id.value) {
            transaction(database) {
                val backend = selectBackend(preferredVideoCodec)

                val pipeline = Pipeline(
                    10,
                    SoftwareVideoBackend,
                    backend,
                    backend,
                    backend,
                )

                val configuration = TranscodingConfiguration(
                    transcoding,
                    preferredVideoCodec,
                    preferredAudioCodec,
                    preferredSubtitleCodec,
                    pipeline,
                )

                TranscodingJob(
                    item,
                    base.resolve(item.id.value.toHexDashString()),
                    variants(item.video.first { it.index == 0 }),
                    configuration,
                )
            }
        }
}
