package dev.scriptor.rest

import dev.scriptor.HlsCache
import dev.scriptor.context.AuthContext
import dev.scriptor.model.Bearer
import dev.scriptor.model.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.result.ChannelResult
import dev.scriptor.server.result.Result
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.time.Duration.ofMinutes
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.extension
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

@Endpoint("/media")
class MediaRest {

    @Resource("/", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMediaList(@Header authorization: Bearer): List<Media> = transaction(database) {
        auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        Media.all().toList()
    }

    @Resource("/[id]", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Bearer,
    ): Media = transaction(database) {
        auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        Media.findById(id)
            ?: throw NotFoundSignal()
    }

    @Resource("/stream/[id]")
    context(database: Database, auth: AuthContext)
    fun getMediaStream(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
        @Header range: String?,
    ): Result = transaction(database) {
        val now = now()
        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = Media.findById(id)
            ?: throw NotFoundSignal()

        val channel = FileChannel.open(item.path)
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
        session.expiresAt = now + ofMinutes(60).toKotlinDuration()
        session.sequence = sequence
        session.next = begin + count
        val limit = begin + count - 1

        val headers = ParameterList()
        headers["accept-ranges"] = "bytes"
        headers["content-range"] = "bytes $begin-$limit/$total"

        ChannelResult(
            206,
            "Partial Content",
            when (item.path.extension) {
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

    val hlsUriLine = """^(?!#)(?!\s*$)(.+)$""".toRegex()

    fun appendToken(uri: String, token: String): String {
        val separator = if ('?' in uri) '&' else '?'
        return "${uri}${separator}token=${token}"
    }

    fun appendToken(path: Path, token: String): String {
        return path
            .bufferedReader()
            .useLines { lines ->
                lines.joinToString("\n") { line ->
                    hlsUriLine.replace(line) { match ->
                        appendToken(match.value, token)
                    }
                }
            }
    }

    @Resource("/stream/[id]/master.m3u8", result = "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        hls: HlsCache,
        auth: AuthContext,
    )
    fun getMediaStreamMaster(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
    ): String = transaction(database) {
        val now = now()
        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = Media.findById(id)
            ?: throw NotFoundSignal()

        session.access = now
        session.expiresAt = now + ofMinutes(60).toKotlinDuration()

        val job = hls.job(item)
        val path = job.master()

        appendToken(path, token)
    }

    @Resource("/stream/[id]/[name]/index.m3u8", result = "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        hls: HlsCache,
        auth: AuthContext,
    )
    fun getMediaStreamIndex(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @QueryParameter token: String,
    ): String = transaction(database) {
        val now = now()
        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = Media.findById(id)
            ?: throw NotFoundSignal()

        session.access = now
        session.expiresAt = now + ofMinutes(60).toKotlinDuration()

        val job = hls.job(item)
        val path = job.index(name)

        appendToken(path, token)
    }

    @Resource("/stream/[id]/[name]/segment[index].ts")
    context(
        _: Logger,
        database: Database,
        hls: HlsCache,
        auth: AuthContext,
    )
    fun getMediaStreamSegment(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @PathParameter index: Long,
        @QueryParameter token: String,
        @Header range: String?,
    ): Result = transaction(database) {
        val now = now()
        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = Media.findById(id)
            ?: throw NotFoundSignal()

        session.access = now
        session.expiresAt = now + ofMinutes(60).toKotlinDuration()

        val job = hls.job(item)
        val path = job.segment(name, index)

        val channel = FileChannel.open(path)
        val total = channel.size()

        if (range.isNullOrBlank()) {
            ChannelResult(
                200,
                "OK",
                "video/mp2t",
                ParameterList(),
                channel,
                0L,
                total,
            )
        } else {
            val segment = range
                .substringAfter("bytes=")
                .split('-')
                .filter { it.isNotBlank() }

            val begin = segment[0].toLong()
            val end =
                if (segment.size == 2)
                    segment[1].toLong()
                else null

            val count = minOf(
                if (end !== null) end - begin
                else 2048L * 1024L,
                total - begin,
            )

            val limit = begin + count - 1

            val headers = ParameterList()
            headers["accept-ranges"] = "bytes"
            headers["content-range"] = "bytes $begin-$limit/$total"

            ChannelResult(
                206,
                "Partial Content",
                "video/mp2t",
                headers,
                channel,
                begin,
                count,
            )
        }
    }
}
