package dev.scriptor.rest

import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Header
import dev.scriptor.server.annotation.PathParameter
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultChannel
import java.nio.channels.FileChannel
import java.nio.file.Path

@Endpoint("/media")
class MediaRest {

    @Resource("/[id]")
    fun getMediaById(@PathParameter("id") id: String, @Header("Range") range: String?): HTTPResult<*> {

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

        val count = minOf(if (end !== null) end - begin else (128L * 1024L), total - begin)

        val limit = begin + count - 1

        val headers: MutableMap<String, String> = HashMap()
        headers["Accept-Ranges"] = "bytes"
        headers["Content-Type"] = "video/x-matroska"
        headers["Content-Length"] = count.toString()
        headers["Content-Range"] = "bytes $begin-$limit/$total"

        return HTTPResultChannel(206, "Partial Content", headers, channel, begin, count)
    }
}
