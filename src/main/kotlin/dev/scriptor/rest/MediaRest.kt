package dev.scriptor.rest

import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultChannel
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.time.Clock.System.now

@Endpoint("/media")
class MediaRest {

    @Inject("log")
    var log: Logger = Logger.getGlobal()

    data class Session(
        val time: Long,
        val next: Long,
    )

    val sessions: MutableMap<String, Session> = HashMap()

    @Resource("/[id]")
    fun getMediaById(
        @PathParameter("id") id: String,
        @QueryParameter("session") key: String?,
        @Header("Range") range: String?,
    ): HTTPResult<*> {

        val path = Path.of("/home/felix/Videos/Noisestorm - Crab Rave (Official Music Video).mkv")
        val channel = FileChannel.open(path)

        val total = channel.size()

        val begin: Long
        val end: Long?

        if (range.isNullOrBlank()) {
            begin = 0L
            end = null
        } else {
            val segment = range
                .substringAfter("bytes=")
                .split('-')
                .filter { it.isNotBlank() }

            begin = segment[0].toLong()
            end =
                if (segment.size == 2) segment[1].toLong()
                else null
        }

        val now = now().toEpochMilliseconds()

        val chunk: Long
        if (key == null || key !in sessions) {
            chunk = 1024L * 1024L
        } else {
            val (time, next) = sessions[key]!!

            if (next != begin) {
                chunk = 1024L * 1024L
            } else {
                val metric = maxOf(0L, minOf(800L, now - time - 200L)) / 100L + 1L

                chunk = metric * 1024L * 1024L
            }
        }

        val count = minOf(
            if (end !== null) end - begin
            else chunk,
            total - begin,
        )

        val limit = begin + count - 1

        if (key != null) {
            sessions[key] = Session(now, begin + count)
        }

        val headers: MutableMap<String, String> = HashMap()
        headers["Accept-Ranges"] = "bytes"
        headers["Content-Type"] = "video/x-matroska"
        headers["Content-Length"] = count.toString()
        headers["Content-Range"] = "bytes $begin-$limit/$total"

        return HTTPResultChannel(
            206,
            "Partial Content",
            headers,
            channel,
            begin,
            count,
        )
    }
}
