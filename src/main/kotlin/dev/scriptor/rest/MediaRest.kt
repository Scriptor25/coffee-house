package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.MediaContext
import dev.scriptor.model.Bearer
import dev.scriptor.model.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultChannel
import java.nio.channels.FileChannel
import kotlin.io.path.extension
import kotlin.time.Clock.System.now
import kotlin.uuid.Uuid

@Endpoint("/media")
class MediaRest {

    @Inject("auth")
    lateinit var auth: AuthContext

    @Inject("media")
    lateinit var media: MediaContext

    @Resource("/", result = "application/json")
    fun getMediaList(@Header("authorization") bearer: Bearer): List<Media> {
        auth.auth(bearer.token)
            ?: throw UnauthorizedSignal()

        return media.getAllMedia()
    }

    @Resource("/[id]", result = "application/json")
    fun getMediaById(@PathParameter("id") id: Uuid, @Header("authorization") bearer: Bearer): Media {
        auth.auth(bearer.token)
            ?: throw UnauthorizedSignal()

        return media.getMediaById(id)
            ?: throw NotFoundSignal(content = "no media for id $id")
    }

    @Resource("/stream/[id]")
    fun getMediaStreamById(
        @PathParameter("id") id: Uuid,
        @QueryParameter("token") token: String,
        @Header("range") range: String?,
    ): HTTPResult<*> {
        val now = now()
        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val media = media.getMediaById(id)
            ?: throw NotFoundSignal(content = "no media for id $id")

        val channel = FileChannel.open(media.path)
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
                if (segment.size == 2)
                    segment[1].toLong()
                else null
        }

        val chunk: Long
        val sequence: Long

        if (session.next == begin && session.access != null && (now - session.access!!).inWholeSeconds < 30L) {
            val metric = maxOf(0L, minOf(7L, session.sequence)) + 1L

            chunk = metric * 512L * 1024L
            sequence = session.sequence + 1L
        } else {
            chunk = 512L * 1024L
            sequence = 0L
        }

        val count = minOf(
            if (end !== null) end - begin
            else chunk,
            total - begin,
        )

        session.access = now
        session.sequence = sequence
        session.next = begin + count

        val limit = begin + count - 1

        val headers = ParameterList()
        headers["accept-ranges"] = "bytes"
        headers["content-range"] = "bytes $begin-$limit/$total"

        return HTTPResultChannel(
            206,
            "Partial Content",
            when (media.path.extension) {
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                else -> "*/*"
            },
            headers,
            channel,
            begin,
            count,
        )
    }
}
