package dev.scriptor.rest

import dev.scriptor.model.MediaModel
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultChannel
import dev.scriptor.server.http.result.HTTPResultVoid
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.sql.Connection
import java.util.logging.Logger
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

@Endpoint("/media")
class MediaRest {

    @Inject("log")
    lateinit var log: Logger

    @Inject("connection")
    lateinit var connection: Connection

    data class Session(
        val time: Long,
        val sequence: Long,
        val next: Long,
    )

    val sessions: MutableMap<String, Session> = HashMap()
    val cache: MutableMap<Uuid, MediaModel> = HashMap()

    fun getMediaById(id: Uuid): MediaModel? {
        if (id in cache) return cache[id]
        val value = connection.prepareStatement(
            """
            select id, path, modified from media
            where id = ?
            limit 1
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, id.toHexDashString())
            statement.executeQuery().use {
                if (it.next())
                    MediaModel(
                        Uuid.parseHexDash(it.getString("id")),
                        Path.of(it.getString("path")),
                        it.getTimestamp("modified").toInstant().toKotlinInstant(),
                    )
                else null
            }
        }
        if (value != null) {
            cache[id] = value
        }
        return value
    }

    @Resource("/[id]")
    fun getMediaById(
        @PathParameter("id") id: Uuid,
        @QueryParameter("session") key: String?,
        @Header("Range") range: String?,
    ): HTTPResult<*> {
        val media = getMediaById(id) ?: return HTTPResultVoid(404, "Not Found")

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
                if (segment.size == 2) segment[1].toLong()
                else null
        }

        val now = now().toEpochMilliseconds()

        val chunk: Long
        val seq: Long
        if (key == null || key !in sessions) {
            chunk = 1024L * 1024L
            seq = 0L
        } else {
            val (time, sequence, next) = sessions[key]!!

            if (next != begin || (now - time) > 30_000L) {
                chunk = 1024L * 1024L
                seq = 0L
            } else {
                val metric = maxOf(0L, minOf(7L, sequence)) + 1L

                chunk = metric * 1024L * 1024L
                seq = sequence + 1L
            }
        }

        val count = minOf(
            if (end !== null) end - begin
            else chunk,
            total - begin,
        )

        val limit = begin + count - 1

        if (key != null) {
            sessions[key] = Session(now, seq, begin + count)
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
