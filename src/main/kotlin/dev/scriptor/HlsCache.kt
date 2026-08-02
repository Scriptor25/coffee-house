package dev.scriptor

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.uuid.Uuid

class HlsCache(private val base: Path) {

    private data class Job(
        val cache: Path,
        val process: Process,
    )

    private val jobs = ConcurrentHashMap<String, Job>()

    fun prepare(id: Uuid, res: String, path: Path): Path {
        val key = "${id.toHexDashString()}/$res"
        val cache = base.resolve(id.toHexDashString()).resolve(res)

        cache.createDirectories()

        start(key, path, cache)
        return cache
    }

    fun segment(id: Uuid, res: String, index: Long): Path? = file(id, res, "segment$index.ts")

    fun file(id: Uuid, res: String, filename: String): Path? {
        val key = "${id.toHexDashString()}/$res"
        val cache = base.resolve(id.toHexDashString()).resolve(res)

        val path = cache.resolve(filename)

        while (path.notExists()) {
            Thread.onSpinWait()

            val job = jobs[key]

            if (job != null && !job.process.isAlive) {
                break
            }
        }

        return if (path.exists()) path else null
    }

    private fun start(key: String, path: Path, cache: Path) {
        jobs.computeIfAbsent(key) {
            val command = listOf(
                "ffmpeg",

                "-i", path.absolutePathString(),

                "-c:v", "libx264",
                "-c:a", "aac",

                "-preset", "veryfast",

                "-f", "segment",

                "-g", "180",
                "-keyint_min", "180",
                "-sc_threshold", "0",

                "-segment_time", "6",
                "-segment_format", "mpegts",

                cache.resolve("segment%d.ts").absolutePathString(),
            )

            val process = ProcessBuilder(command).start()

            Job(cache, process)
        }
    }
}