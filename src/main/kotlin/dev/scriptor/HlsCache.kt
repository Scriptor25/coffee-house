package dev.scriptor

import dev.scriptor.model.MediaMetadata
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.uuid.Uuid

class HlsCache(
    private val base: Path,
    private val disableTranscoding: Boolean,
) {

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

    private val definedVariants = listOf(
        Variant("2160p", 3840, 2160, 16_000_000L, Profile.HIGH),
        Variant("1440p", 2560, 1440, 8_000_000L, Profile.HIGH),
        Variant("1080p", 1920, 1080, 6_000_000L, Profile.MEDIUM),
        Variant("720p", 1280, 720, 3_000_000L, Profile.MEDIUM),
        Variant("480p", 854, 480, 1_500_000L, Profile.LOW),
        Variant("360p", 640, 360, 750_000L, Profile.LOW),
        Variant("144p", 256, 144, 300_000L, Profile.POTATO),
    )

    private data class Job(
        val cache: Path,
        val result: Path,
        val process: Process?,
    )

    private val processes: MutableMap<Pair<Uuid, String>, Job> = ConcurrentHashMap()
    private val segmentation: MutableMap<Pair<Uuid, String>, Job> = ConcurrentHashMap()

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

        if (disableTranscoding) return result

        for (variant in definedVariants) {
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

    fun available(id: Uuid): List<String> {
        val names = mutableListOf<String>()
        for ((key, proc) in processes) {
            if (key.first != id) continue
            if (proc.process != null) {
                if (proc.process.isAlive) continue
                if (proc.process.exitValue() != 0) continue
            }
            names += key.second
        }

        return names.filter {
            val job = segmentation[id to it]
            job != null && (job.process == null || (!job.process.isAlive && job.process.exitValue() == 0))
        }
    }

    fun prepare(item: MediaMetadata, variants: List<Variant>, segment: Long) {
        if (variants.isEmpty()) return

        val sorted = variants.sortedBy(Variant::height)
        val segment = segment.toDouble() / 1000.0
        val cache = base.resolve(item.id.toHexDashString())

        enqueue(
            item.id,
            item.path,
            cache,
            segment,
            sorted.subList(0, 1),
        )

        val key = item.id to sorted[0].name
        while (key !in segmentation) {
            Thread.onSpinWait()
        }

        awaitJob(key)

        enqueue(
            item.id,
            item.path,
            cache,
            segment,
            sorted.subList(1, sorted.size),
        )
    }

    fun index(key: Pair<Uuid, String>): Path = file(key, "index.m3u8")

    fun segment(key: Pair<Uuid, String>, index: Long): Path = file(key, "segment$index.ts")

    private fun awaitProcess(key: Pair<Uuid, String>): Job {
        val proc = processes[key]
            ?: throw Error("process $key does not exist")

        if (proc.process != null && proc.process.waitFor() != 0) {
            throw Error("process $key failed")
        }

        return proc
    }

    private fun awaitJob(key: Pair<Uuid, String>): Job {
        awaitProcess(key)

        val job = segmentation[key]
            ?: throw Error("segmentation $key does not exist")

        if (job.process != null && job.process.waitFor() != 0) {
            throw Error("segmentation $key failed")
        }

        return job
    }

    private fun file(key: Pair<Uuid, String>, filename: String): Path {
        val job = awaitJob(key)

        return job.cache.resolve(filename)
    }

    private fun generateVariants(id: Uuid, path: Path, cache: Path, variants: List<Variant>) {
        if (variants.isEmpty()) return

        if (disableTranscoding) {
            for ((name) in variants) {
                processes[id to name] = Job(
                    cache,
                    path,
                    null,
                )
            }

            return
        }

        val splitter = mutableListOf<String>()
        val scaler = mutableListOf<String>()
        val mapper = mutableListOf<String>()

        val exists = mutableSetOf<String>()

        for ((index, variant) in variants.withIndex()) {
            val variantPath = cache.resolve("${variant.name}.mp4")

            if (variantPath.exists()) {
                exists += variant.name
            } else {
                splitter += "[v${index + 1}]"
                scaler += "[v${index + 1}]scale=${variant.width}:${variant.height}[v${variant.height}]"

                mapper += listOf(
                    "-map", "[v${variant.height}]", "-map", "0:a",
                    "-c:v", "libx264",
                    "-b:v", "${variant.bitrate}",
                    "-c:a", "aac",
                    "-maxrate", "${variant.bitrate}",
                    "-bufsize", "${variant.bitrate * 2}",
                    "-preset", variant.profile.preset,
                    "-crf", variant.profile.crf.toString(),
                    variantPath.absolutePathString(),
                )
            }
        }

        cache.createDirectories()

        val split = "[0:v]split=${variants.size}" + splitter.joinToString("")
        val scale = scaler.joinToString(";")
        val map = mapper.toTypedArray()

        val command = listOf(
            "ffmpeg",
            "-i", path.absolutePathString(),
            "-filter_complex", "$split;$scale",
            *map,
        )

        val process = ProcessBuilder(command)
            .redirectError(INHERIT)
            .start()

        for ((name) in variants) {
            processes[id to name] = Job(
                cache,
                cache.resolve("$name.mp4"),
                if (name in exists) null else process,
            )
        }
    }

    private fun generateSegments(id: Uuid, name: String, src: Path, dst: Path, segment: Double) {
        val index = dst.resolve("index.m3u8")

        val process: Process?
        if (index.exists()) {
            process = null
        } else {
            dst.createDirectories()

            val command = listOf(
                "ffmpeg",
                "-i", src.absolutePathString(),
                "-c", "copy",
                "-f", "hls",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_time", "$segment",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", dst.resolve("segment%d.ts").absolutePathString(),
                index.absolutePathString(),
            )

            process = ProcessBuilder(command)
                .redirectError(INHERIT)
                .start()
        }

        segmentation[id to name] = Job(
            dst,
            index,
            process,
        )
    }

    private fun enqueue(
        id: Uuid,
        path: Path,
        cache: Path,
        segment: Double,
        variants: List<Variant>,
    ) {
        if (variants.isEmpty()) return

        val toProcess = variants.filter { (id to it.name) !in processes }
        val toSegmentation = variants.filter { (id to it.name) !in segmentation }

        if (toProcess.isEmpty() && toSegmentation.isEmpty()) return

        generateVariants(id, path, cache, toProcess)

        for ((name) in toSegmentation) {
            Thread {
                val job = awaitProcess(id to name)

                val src = job.result
                val dst = cache.resolve(name)

                generateSegments(id, name, src, dst, segment)
            }.start()
        }
    }
}