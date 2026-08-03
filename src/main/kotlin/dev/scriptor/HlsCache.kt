package dev.scriptor

import dev.scriptor.model.MediaMetadata
import org.json.JSONObject
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.uuid.Uuid

class HlsCache(private val base: Path) {

    enum class Profile(
        val preset: String,
        val crf: Int,
    ) {
        LOSSLESS("veryslow", 18),
        HIGH("slow", 20),
        MEDIUM("medium", 22),
        LOW("veryfast", 24),
        POTATO("superfast", 26),
    }

    data class Variant(
        val name: String,
        val width: Int,
        val height: Int,
        val bitrate: Long,
        val profile: Profile,
    )

    val variants = listOf(
        Variant("2160p", 3840, 2160, 16_000_000L, Profile.HIGH),
        Variant("1440p", 2560, 1440, 8_000_000L, Profile.HIGH),
        Variant("1080p", 1920, 1080, 6_000_000L, Profile.MEDIUM),
        Variant("720p", 1280, 720, 3_000_000L, Profile.MEDIUM),
        Variant("480p", 854, 480, 1_500_000L, Profile.LOW),
        Variant("360p", 640, 360, 750_000L, Profile.LOW),
        Variant("144p", 256, 144, 300_000L, Profile.POTATO),
    )

    data class Job(
        val cache: Path,
        val keyframes: List<Double>,
        val boundaries: List<Pair<Double, Double>>,
        val process: Process,
        val processSegments: Map<String, Process>,
    )

    private val jobs = ConcurrentHashMap<Uuid, Job>()

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
        for (variant in variants) {
            if (variant.width < item.width && variant.height < item.height) {
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
        return result
    }

    fun prepare(item: MediaMetadata, variants: List<Variant>, segment: Long): Job {
        return enqueue(
            item.id,
            item.path,
            item.duration / 1000.0,
            segment / 1000.0,
            variants,
            base.resolve(item.id.toHexDashString()),
        )
    }

    fun segment(id: Uuid, name: String, index: Long): Path? = file(id, name, "segment$index.ts")

    fun file(id: Uuid, name: String, filename: String): Path? {
        val path = base
            .resolve(id.toHexDashString())
            .resolve(name)
            .resolve(filename)

        while (path.notExists()) {
            Thread.onSpinWait()

            val job = jobs[id]
            if (job != null && !job.process.isAlive) {
                break
            }
        }

        return if (path.exists()) path else null
    }

    fun keyframes(path: Path): List<Double> {
        val command = listOf(
            "ffprobe",
            "-skip_frame", "nokey",
            "-select_streams", "v:0",
            "-show_entries", "frame=pts_time",
            "-of", "json",
            path.absolutePathString(),
        )

        val process = ProcessBuilder(command)
            .redirectError(INHERIT)
            .start()

        val json = JSONObject(process.inputStream.bufferedReader().readText())

        val code = process.waitFor()
        if (code != 0) throw Error("failed to extract keyframes")

        val frames = json.getJSONArray("frames")

        val keyframes = mutableListOf<Double>()
        for (index in 0 until frames.length()) {
            val frame = frames.getJSONObject(index)
            val ptsTime = frame.getString("pts_time").toDouble()
            keyframes += ptsTime
        }
        return keyframes
    }

    fun boundaries(keyframes: List<Double>, duration: Double, segment: Double): List<Pair<Double, Double>> {
        val boundaries = mutableListOf(0.0)
        var next = segment

        for (keyframe in keyframes) {
            if (keyframe >= next) {
                boundaries += keyframe
                next += segment
            }
        }

        boundaries += duration
        return boundaries.zipWithNext()
    }

    private fun enqueue(
        id: Uuid,
        path: Path,
        duration: Double,
        segment: Double,
        variants: List<Variant>,
        cache: Path,
    ): Job {
        return jobs.computeIfAbsent(id) {
            cache.createDirectories()

            val keyframes = keyframes(path)
            val boundaries = boundaries(keyframes, duration, segment)

            val split = "[0:v]split=${variants.size}" +
                    List(variants.size) { index -> "[v${index + 1}]" }
                        .joinToString("")

            val filter = variants
                .mapIndexed { index, variant ->
                    "[v${index + 1}]scale=${variant.width}:${variant.height}[v${variant.height}]"
                }
                .joinToString(";")

            val map = variants.flatMap { variant ->
                listOf(
                    "-map", "[v${variant.height}]",
                    "-c:v", "libx264",
                    "-b:v", "${variant.bitrate}",
                    "-maxrate", "${variant.bitrate}",
                    "-bufsize", "${variant.bitrate * 2}",
                    "-preset", variant.profile.preset,
                    "-crf", variant.profile.crf.toString(),
                    "-c:a", "aac",
                    cache.resolve("${variant.name}.mp4").absolutePathString(),
                )
            }.toTypedArray()

            val command = listOf(
                "ffmpeg",
                "-i", path.absolutePathString(),
                "-filter_complex", "$split;$filter",
                *map,
            )

            val process = ProcessBuilder(command)
                .redirectError(INHERIT)
                .start()

            // TODO: await filter process before starting segmentation

            // segment item per variant
            val processSegments = mutableMapOf<String, Process>()
            for (variant in variants) {
                val dst = cache.resolve(variant.name)
                dst.createDirectories()

                val command = listOf(
                    "ffmpeg",
                    "-i", cache.resolve("${variant.name}.mp4").absolutePathString(),
                    "-c", "copy",
                    "-f", "segment",
                    "-segment_time", "$segment",
                    "-segment_format", "mpegts",
                    dst.resolve("segment%d.ts").absolutePathString(),
                )

                val process = ProcessBuilder(command)
                    .redirectError(INHERIT)
                    .start()

                processSegments[variant.name] = process
            }

            Job(cache, keyframes, boundaries, process, processSegments)
        }
    }
}