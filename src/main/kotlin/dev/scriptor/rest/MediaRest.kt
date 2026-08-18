package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.TranscodingCache
import dev.scriptor.context.AuthContext
import dev.scriptor.jsonOf
import dev.scriptor.model.Authorization
import dev.scriptor.model.Chapter
import dev.scriptor.model.Media
import dev.scriptor.server.*
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

@Controller("/media")
class MediaRest {

    private val hlsUriLine = """^(?!#)(?!\s*$)(.+)$""".toRegex()
    private val hlsUriTag = """(\bURI=")([^"]+)(")""".toRegex()

    private fun appendToken(uri: String, token: String): String {
        val separator = if ('?' in uri) '&' else '?'
        return "${uri}${separator}token=${token}"
    }

    private fun appendToken(path: Path, token: String): List<String> = path
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

    context(
        database: Database,
        auth: AuthContext,
    )
    private fun mediaSession(
        id: Uuid,
        authorization: Authorization?,
        token: String? = null,
    ): Media {
        val instant = now()

        val token = when {
            authorization != null && authorization.scheme == "Bearer" -> authorization.credentials
            else -> token
        } ?: throw UnauthorizedSignal()

        val session = auth.auth(token, instant)
            ?: throw UnauthorizedSignal()

        val item = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        transaction(database) {
            session.access = instant
            session.expiresAt = instant + ofMinutes(60).toKotlinDuration()
        }

        return item
    }

    private fun stream(range: String?, path: Path): Result {
        val channel = FileChannel.open(path)

        return if (range.isNullOrBlank()) {
            ChannelResult(value = channel)
        } else {
            val total = channel.size()

            val range = range
                .substringAfter("bytes=")
                .split("-", limit = 2)
                .filter { it.isNotBlank() }

            val begin = range[0].toLong()
            val end = if (range.size == 2) range[1].toLong() else (total - 1L)

            if (begin < 0 || end < 0 || begin >= total || end >= total || begin > end) {
                val headers = ParameterList(
                    "content-range" to "bytes */$total",
                )

                RangeNotSatisfiableSignal(headers).generate()
            } else {
                val headers = ParameterList(
                    "content-length" to (end + 1L - begin).toString(),
                    "content-range" to "bytes $begin-$end/$total",
                )

                ChannelResult(
                    206,
                    "Partial Content",
                    headers = headers,
                    value = RangeReadableByteChannel(channel, begin until end),
                )
            }
        }
    }

    @Head("/")
    fun getMediaListHeaders() {
        val headers = ParameterList(
            "content-type" to "application/json",
        )

        throw NoContentSignal(headers)
    }

    @Get("/", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMediaList(
        @QueryParameter offset: Long?,
        @QueryParameter limit: Int?,
        @Header authorization: Authorization,
    ): List<Media> {
        val instant = now()

        val token = when {
            authorization.scheme == "Bearer" -> authorization.credentials
            else -> null
        } ?: throw UnauthorizedSignal()

        val session = auth.auth(token, instant)
            ?: throw UnauthorizedSignal()

        transaction(database) {
            session.access = instant
            session.expiresAt = instant + ofMinutes(60).toKotlinDuration()
        }

        return transaction(database) {
            Media
                .all()
                .offset(offset ?: 0L)
                .limit(limit ?: Int.MAX_VALUE)
                .toList()
        }
    }

    @Head("/[id]")
    fun getMediaHeaders() {
        val headers = ParameterList(
            "content-type" to "application/json",
        )

        throw NoContentSignal(headers)
    }

    @Get("/[id]", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Authorization,
    ): Media {
        return mediaSession(id, authorization)
    }

    @Head("/stream/[id]")
    fun getMediaStreamHeaders() {
        val headers = ParameterList(
            "content-type" to "video/*",
            "accept-ranges" to "bytes",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]", result = "video/*")
    context(database: Database, auth: AuthContext)
    fun getMediaStream(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
        @Header authorization: Authorization?,
        @Header range: String?,
    ): Result {
        val item = mediaSession(id, authorization, token)

        return stream(range, item.path)
    }

    @Head("/stream/[id]/master.m3u8")
    fun getMediaStreamMasterHeaders() {
        val headers = ParameterList(
            "content-type" to "application/vnd.apple.mpegurl",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]/master.m3u8", result = "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
        auth: AuthContext,
    )
    fun getMediaStreamMaster(
        @PathParameter id: Uuid,
        @QueryParameter token: String,
        @Header authorization: Authorization?,
    ): String {
        val item = mediaSession(id, authorization, token)

        val job = transcoding.job(item)
        val path = job.master()

        val manifest = appendToken(path, token).toMutableList()
        manifest += """#EXT-X-SESSION-DATA:DATA-ID="com.apple.hls.chapters",URI="chapters.json?token=$token""""

        return manifest.joinToString("\n")
    }

    @Head("/stream/[id]/[name]/index.m3u8")
    fun getMediaStreamIndexHeaders() {
        val headers = ParameterList(
            "content-type" to "application/vnd.apple.mpegurl",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]/[name]/index.m3u8", result = "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
        auth: AuthContext,
    )
    fun getMediaStreamIndex(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @QueryParameter token: String,
        @Header authorization: Authorization?,
    ): String {
        val item = mediaSession(id, authorization, token)

        val job = transcoding.job(item)
        val path = job.index(name)

        return appendToken(path, token).joinToString("\n")
    }

    @Head("/stream/[id]/[name]/[segment].mp4")
    fun getMediaStreamSegmentHeaders() {
        val headers = ParameterList(
            "content-type" to "video/mp4",
            "accept-ranges" to "bytes",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]/[name]/[segment].mp4", result = "video/mp4")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
        auth: AuthContext,
    )
    fun getMediaStreamSegment(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @PathParameter segment: String,
        @QueryParameter token: String,
        @Header authorization: Authorization?,
        @Header range: String?,
    ): Result {
        val item = mediaSession(id, authorization, token)

        val job = transcoding.job(item)
        val path = job.segment(name, segment)

        return stream(range, path)
    }

    @Head("/stream/[id]/chapters.json")
    fun getMediaStreamChaptersHeaders() {
        val headers = ParameterList(
            "content-type" to "application/json",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]/chapters.json", result = "application/json")
    context(database: Database, auth: AuthContext)
    fun getMediaStreamChapters(
        @PathParameter id: Uuid,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
    ): JsonNode {
        val item = mediaSession(id, authorization, token)

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
