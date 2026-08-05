package dev.scriptor

import dev.scriptor.model.MediaMetadata
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class HlsCache(
    private val base: Path,
    private val transcoding: Boolean,
    private val allowed: Set<String> = setOf("2160p", "1440p", "1080p", "720p", "480p", "360p", "144p"),
) {
    private val definedVariants = listOf(
        Variant("2160p", 3840, 2160, 16_000_000L, Profile.HIGH),
        Variant("1440p", 2560, 1440, 8_000_000L, Profile.HIGH),
        Variant("1080p", 1920, 1080, 6_000_000L, Profile.MEDIUM),
        Variant("720p", 1280, 720, 3_000_000L, Profile.MEDIUM),
        Variant("480p", 854, 480, 1_500_000L, Profile.LOW),
        Variant("360p", 640, 360, 750_000L, Profile.LOW),
        Variant("144p", 256, 144, 300_000L, Profile.POTATO),
    )

    private val jobs: MutableMap<Uuid, TranscodingJob> = ConcurrentHashMap()

    fun variants(item: MediaMetadata): List<Variant> {
        val result = mutableListOf(
            Variant(
                "original",
                item.width - (item.width % 2),
                item.height - (item.height % 2),
                if (item.bitrate == 0L)
                    item.size * 8L * 1000L / item.duration
                else item.bitrate,
                Profile.LOSSLESS,
            )
        )

        if (transcoding) {
            for (variant in definedVariants) {
                if (variant.name in allowed && variant.width < item.width && variant.height < item.height) {
                    val aspect = item.width.toDouble() / item.height.toDouble()
                    val width = (variant.height * aspect).toInt()
                    val height = variant.height

                    result += Variant(
                        variant.name,
                        width - (width % 2),
                        height - (height % 2),
                        variant.bitrate,
                        variant.profile,
                    )
                }
            }
        }

        return result
    }

    fun job(item: MediaMetadata): TranscodingJob =
        jobs.computeIfAbsent(item.id) {
            TranscodingJob(
                item,
                base.resolve(item.id.toHexDashString()),
                variants(item),
                transcoding,
            )
        }
}
