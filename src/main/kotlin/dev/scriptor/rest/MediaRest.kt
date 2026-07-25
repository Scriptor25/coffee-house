package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.getMedia
import dev.scriptor.model.Cookie
import dev.scriptor.model.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.ParameterList
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultChannel
import java.nio.channels.FileChannel
import java.sql.Connection
import java.util.logging.Logger
import kotlin.io.path.extension
import kotlin.time.Clock.System.now
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Endpoint("/media")
class MediaRest {

    @Inject("log")
    lateinit var log: Logger

    @Inject("connection")
    lateinit var connection: Connection

    @Inject("sessions")
    lateinit var sessions: SessionContext

    val cache: MutableMap<Uuid, Media> = HashMap()

    @Resource("/list", result = "application/json")
    fun getMediaList(): Array<Media> {
        return connection.prepareStatement("select * from media").use { statement ->
            statement.executeQuery().use {
                val list = mutableListOf<Media>()
                while (it.next()) {
                    list += it.getMedia()
                }
                list.toTypedArray()
            }
        }
    }

    fun getMediaById(id: Uuid): Media? {
        if (id in cache) return cache[id]
        val value = connection.prepareStatement("select * from media where id = ? limit 1").use { statement ->
            statement.setString(1, id.toHexDashString())
            statement.executeQuery().use {
                if (it.next())
                    it.getMedia()
                else null
            }
        }
        if (value != null) {
            cache[id] = value
        }
        return value
    }

    @Resource("/[id]")
    @OptIn(ExperimentalUuidApi::class)
    fun getMediaById(
        @PathParameter("id") id: Uuid,
        @QueryParameter("session") sessionId: Uuid?,
        @Header("cookie") cookie: Cookie?,
        @Header("range") range: String?,
    ): HTTPResult<*> {
        val media = getMediaById(id)
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

        val now = now()

        val chunk: Long
        val sequence: Long

        val sid: Uuid
        if (sessionId != null)
            sid = sessionId
        else if (cookie != null && "x-session-id" in cookie) {
            val id = cookie["x-session-id"]!!
            sid = Uuid.parseHexDash(id)
        } else {
            sid = Uuid.generateV7()
        }

        val session = sessions[sid] ?: sessions.create(sid)

        if (session.next == begin && now.minus(session.access).inWholeSeconds < 30L) {
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
        headers["content-type"] = when (media.path.extension) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            else -> "*/*"
        }
        headers["content-length"] = count.toString()
        headers["content-range"] = "bytes $begin-$limit/$total"

        headers["set-cookie"] = "x-session-id=${session.id}"

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
