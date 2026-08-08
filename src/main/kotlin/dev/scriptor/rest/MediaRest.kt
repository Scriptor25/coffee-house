package dev.scriptor.rest

import dev.scriptor.HlsCache
import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.jsonOf
import dev.scriptor.model.Bearer
import dev.scriptor.model.Chapter
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
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

@Endpoint("/media")
class MediaRest {

    val hlsUriLine = """^(?!#)(?!\s*$)(.+)$""".toRegex()
    val hlsUriTag = """(\bURI=")([^"]+)(")""".toRegex()

    fun appendToken(uri: String, token: String): String {
        val separator = if ('?' in uri) '&' else '?'
        return "${uri}${separator}token=${token}"
    }

    fun appendToken(path: Path, token: String): List<String> = path
        .bufferedReader()
        .useLines { lines ->
            lines
                .map { line ->
                    when {
                        hlsUriLine.matches(line) -> hlsUriLine.replace(line) { match ->
                            appendToken(match.value, token)
                        }

                        "URI=" in line -> hlsUriTag.replace(line) { match ->
                            buildString {
                                append(match.groupValues[1])
                                append(appendToken(match.groupValues[2], token))
                                append(match.groupValues[3])
                            }
                        }

                        else -> line
                    }
                }
                .toList()
        }

    fun stream(range: String?, path: Path, chunk: Long): Result {
        val channel = FileChannel.open(path)
        val total = channel.size()

        return if (range.isNullOrBlank()) {
            ChannelResult(
                value = channel,
                position = 0L,
                count = total,
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
                else chunk,
                total - begin,
            )

            val limit = begin + count - 1

            val headers = ParameterList()
            headers["accept-ranges"] = "bytes"
            headers["content-range"] = "bytes $begin-$limit/$total"

            ChannelResult(
                206,
                "Partial Content",
                headers = headers,
                value = channel,
                position = begin,
                count = count,
            )
        }
    }

    @Resource("/", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMediaList(
        @QueryParameter offset: Long?,
        @QueryParameter limit: Int?,
        @Header authorization: Bearer,
    ): List<Media> {
        auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        return transaction(database) {
            Media
                .all()
                .offset(offset ?: 0L)
                .limit(limit ?: Int.MAX_VALUE)
                .toList()
        }
    }

    @Resource("/[id]", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Bearer,
    ): Media {
        auth.auth(authorization.token)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Resource("/stream/[id]", result = "video/*")
    context(database: Database, auth: AuthContext)
    fun getMediaStream(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
        @Header range: String?,
    ): Result {
        val now = now()

        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.expiresAt = now + ofMinutes(60).toKotlinDuration()
            session.access = now
        }

        return stream(range, item.path, 2L * 1024L * 1024L)
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
    ): String {
        val now = now()

        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.access = now
            session.expiresAt = now + ofMinutes(60).toKotlinDuration()
        }

        val job = hls.job(item)
        val path = job.master()

        val manifest = appendToken(path, token).toMutableList()
        manifest += """#EXT-X-SESSION-DATA:DATA-ID="com.apple.hls.chapters",URI="chapters.json?token=$token""""

        return manifest.joinToString("\n")
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
    ): String {
        val now = now()

        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.access = now
            session.expiresAt = now + ofMinutes(60).toKotlinDuration()
        }

        val job = hls.job(item)
        val path = job.index(name)

        return appendToken(path, token).joinToString("\n")
    }

    // TODO: differentiate between video and audio
    @Resource("/stream/[id]/[name]/[segment].mp4", result = "video/mp4")
    context(
        _: Logger,
        database: Database,
        hls: HlsCache,
        auth: AuthContext,
    )
    fun getMediaStreamSegment(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @PathParameter segment: String,
        @QueryParameter token: String,
        @Header range: String?,
    ): Result {
        val now = now()

        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.access = now
            session.expiresAt = now + ofMinutes(60).toKotlinDuration()
        }

        val job = hls.job(item)
        val path = job.segment(name, segment)

        return stream(range, path, 2L * 1024L * 1024L)
    }

    @Resource("/stream/[id]/chapters.json", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMediaStreamChapters(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
    ): JsonNode {
        val now = now()

        val session = auth.auth(token, now)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.access = now
            session.expiresAt = now + ofMinutes(60).toKotlinDuration()
        }

        val chapters = transaction(database) { item.chapters.toList() }

        return jsonOf(
            *chapters
                .sortedBy(Chapter::index)
                .mapIndexed { index, chapter ->
                    jsonOf(
                        "chapter" to jsonOf(chapter.index + 1),
                        "start-time" to jsonOf(chapter.start),
                        "duration" to jsonOf(chapter.end - chapter.start),
                        "titles" to jsonOf(
                            jsonOf(
                                "language" to jsonOf(chapter.language ?: "und"),
                                "title" to jsonOf(chapter.title ?: "Chapter $index"),
                            ),
                        ),
                    )
                }
                .toTypedArray()
        )
    }
}
