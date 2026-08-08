package dev.scriptor

import dev.scriptor.model.Media
import dev.scriptor.model.VideoTrack
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class HlsCache(
    private val base: Path,
    private val transcoding: Boolean,
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
            if (sourceWidth != item.width || sourceHeight != item.height)
                ScaleVariant(
                    "original",
                    sourceWidth,
                    sourceHeight,
                    item.bitRate,
                    Profile.LOSSLESS,
                )
            else
                OriginalVariant()
        )

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

        return result
    }

    fun job(item: Media): TranscodingJob =
        jobs.computeIfAbsent(item.id.value) {
            TranscodingJob(
                item,
                base.resolve(item.id.value.toHexDashString()),
                variants(item.video.first()),
                transcoding,
            )
        }
}
