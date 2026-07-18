package dev.scriptor.rest

import dev.scriptor.RangeInputStream
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultStream
import java.io.FileInputStream
import java.io.InputStream

@Endpoint("/media")
class MediaRest {

    @Resource("/[id]")
    fun getMediaById(@Path("id") id: String, @Header("Range") range: String?, @Body body: InputStream): HTTPResult<*> {

        val base = FileInputStream("/home/felix/Videos/Noisestorm - Crab Rave (Official Music Video).mkv")

        val total = base.available()

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

        base.skip(begin)

        val length = minOf(if (end !== null) end - begin else (64L * 1024L), total.toLong() - begin)
        val stream = RangeInputStream(base, length)

        val limit = begin + length - 1

        val headers: MutableMap<String, String> = HashMap()
        headers["Accept-Ranges"] = "bytes"
        headers["Content-Type"] = "video/x-matroska"
        headers["Content-Length"] = length.toString()
        headers["Content-Range"] = "bytes $begin-$limit/$total"

        return HTTPResultStream(206, "Partial Content", headers, stream)
    }
}