package dev.scriptor

import java.lang.ProcessBuilder.Redirect.INHERIT
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.math.ceil
import kotlin.uuid.Uuid

class HlsCache(private val base: Path) {

    private data class Job(
        val cache: Path,
        val process: Process,
    )

    private val jobs = ConcurrentHashMap<String, Job>()

    fun prepare(id: Uuid, res: String, path: Path, framerate: Double): Path {
        val key = "${id.toHexDashString()}/$res"
        val cache = base.resolve(id.toHexDashString()).resolve(res)

        cache.createDirectories()

        enqueue(key, path, framerate, cache)
        return cache
    }

    fun segment(id: Uuid, res: String, index: Long): Path? = file(id, res, "segment$index")

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

    private fun enqueue(key: String, path: Path, framerate: Double, cache: Path) {
        jobs.computeIfAbsent(key) {
            val length = 6
            val frames = ceil(length * framerate)

            val command = listOf(
                "ffmpeg",

                "-i", path.absolutePathString(),

                "-c:v", "libx264",
                "-c:a", "aac",

                "-preset", "veryfast",

                "-f", "segment",

                "-g", "$frames",
                "-keyint_min", "$frames",
                "-sc_threshold", "0",

                "-force_key_frames", "expr:gte(t,n_forced*$length)",

                "-segment_time", "$length",
                "-segment_format", "mpegts",

                cache.resolve("segment%d").absolutePathString(),
            )

            val process = ProcessBuilder(command).redirectError(INHERIT).start()

            Job(cache, process)
        }
    }
}