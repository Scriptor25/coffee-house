package dev.scriptor

import java.lang.ProcessBuilder.Redirect.INHERIT
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.uuid.Uuid

class HlsCache(private val base: Path) {

    private val locks = ConcurrentHashMap<String, Any>()

    fun prepare(id: Uuid, res: String, path: Path): Path? {
        val cache = base.resolve(id.toHexDashString()).resolve(res)
        val playlist = cache.resolve("index.m3u8")

        if (playlist.exists()) {
            return cache
        }

        val lock = locks.computeIfAbsent("${id.toHexDashString()}/$res") { Any() }

        synchronized(lock) {
            if (playlist.exists()) {
                return cache
            }

            if (!generate(path, cache)) {
                return null
            }
        }

        return cache
    }

    private fun generate(src: Path, dst: Path): Boolean {
        dst.createDirectories()

        val playlist = dst.resolve("index.m3u8")

        val command = listOf(
            "ffmpeg",

            "-i", src.absolutePathString(),

            "-c:v", "libx264",

            "-preset", "veryfast",

            "-c:a", "aac",

            "-hls_time", "6",
            "-hls_playlist_type", "vod",
            "-hls_flags", "independent_segments",
            "-hls_segment_filename", dst.resolve("segment%d.ts").absolutePathString(),

            playlist.absolutePathString()
        )

        val code = ProcessBuilder(command)
            .start()
            .waitFor()

        return code == 0
    }
}