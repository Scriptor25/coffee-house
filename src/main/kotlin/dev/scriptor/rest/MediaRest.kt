package dev.scriptor.rest

import dev.scriptor.TranscodingCache
import dev.scriptor.context.AuthContext
import dev.scriptor.jsonOf
import dev.scriptor.model.Authorization
import dev.scriptor.model.Cookie
import dev.scriptor.model.Session
import dev.scriptor.model.media.Chapter
import dev.scriptor.model.media.Media
import dev.scriptor.server.*
import dev.scriptor.server.annotation.*
import dev.scriptor.server.result.ChannelResult
import dev.scriptor.server.result.Result
import dev.scriptor.server.result.StringResult
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.bufferedReader
import kotlin.io.path.readLines
import kotlin.uuid.Uuid

@Controller("/media")
class MediaRest {

    private val hlsUriLine = """^(?!#)(?!\s*$)(.+)$""".toRegex()
    private val hlsUriTag = """(\bURI=")([^"]+)(")""".toRegex()

    private fun appendToken(uri: String, token: String): String {
        val separator = if ('?' in uri) '&' else '?'
        return "${uri}${separator}token=${token}"
    }

    private fun appendToken(path: Path, token: String?): List<String> =
        if (token == null) path.readLines()
        else path
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

    private data class MediaSession(
        val media: Media,
        val session: Session,
    )

    context(
        database: Database,
        auth: AuthContext,
    )
    private fun mediaSession(
        id: Uuid,
        authorization: Authorization?,
        cookie: Cookie?,
        token: String? = null,
    ): MediaSession {
        val token = when {
            authorization != null && authorization.scheme == "Bearer" -> authorization.credentials
            cookie != null && "session" in cookie -> cookie["session"]
            else -> token
        } ?: throw UnauthorizedSignal()

        val session = auth.auth(token)
            ?: throw UnauthorizedSignal()

        val media = transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()

        return MediaSession(media, session)
    }

    private fun stream(range: String?, path: Path, headers: ParameterList = ParameterList()): Result {
        val channel = FileChannel.open(path)

        if (range.isNullOrBlank()) {
            return ChannelResult(
                headers = headers,
                value = channel,
            )
        }

        val total = channel.size()

        val range = range
            .substringAfter("bytes=")
            .split("-", limit = 2)
            .filter { it.isNotBlank() }

        val begin = range[0].toLong()
        val end = if (range.size == 2) range[1].toLong() else (total - 1L)

        if (begin < 0 || end < 0 || begin >= total || end >= total || begin > end) {
            headers["content-range"] = "bytes */$total"

            throw RangeNotSatisfiableSignal(headers)
        }

        headers["content-length"] = (end + 1L - begin).toString()
        headers["content-range"] = "bytes $begin-$end/$total"

        return ChannelResult(
            206,
            "Partial Content",
            headers = headers,
            value = RangeReadableByteChannel(channel, begin..end),
        )
    }

    @Head("/")
    fun getMediaListHeaders() {
        val headers = ParameterList(
            "content-type" to "application/json",
        )

        throw NoContentSignal(headers)
    }

    @Get("/", result = "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun getMediaList(
        @QueryParameter offset: Long?,
        @QueryParameter limit: Int?,
        @Header authorization: Authorization,
    ): List<Media> {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

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
    context(
        _: Database,
        _: AuthContext,
    )
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Authorization,
        @Header cookie: Cookie?,
    ): Media {
        return mediaSession(id, authorization, cookie).media
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
    context(
        _: Database,
        _: AuthContext,
    )
    fun getMediaStream(
        @PathParameter id: Uuid,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
        @Header cookie: Cookie?,
        @Header range: String?,
    ): Result {
        val (item, session) = mediaSession(id, authorization, cookie, token)

        val headers = ParameterList("set-cookie" to "session=${session.jwt}; max-age=${session.maxAge}; path=/")

        return stream(range, item.path, headers)
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
        _: Database,
        _: AuthContext,
        transcoding: TranscodingCache,
    )
    fun getMediaStreamMaster(
        @PathParameter id: Uuid,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
        @Header cookie: Cookie?,
    ): Result {
        val (item, session) = mediaSession(id, authorization, cookie, token)

        val headers = ParameterList("set-cookie" to "session=${session.jwt}; max-age=${session.maxAge}; path=/")

        val job = transcoding.job(item)
        val path = job.master()

        val manifest = appendToken(path, token).toMutableList()
        manifest += """#EXT-X-SESSION-DATA:DATA-ID="com.apple.hls.chapters",URI="chapters.json?token=$token""""

        val value = manifest.joinToString("\n")

        return StringResult(
            headers = headers,
            value = value,
        )
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
        _: Database,
        _: AuthContext,
        transcoding: TranscodingCache,
    )
    fun getMediaStreamIndex(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
        @Header cookie: Cookie?,
    ): Result {
        val (item, session) = mediaSession(id, authorization, cookie, token)

        val headers = ParameterList("set-cookie" to "session=${session.jwt}; max-age=${session.maxAge}; path=/")

        val job = transcoding.job(item)
        val path = job.index(name)

        val value = appendToken(path, token).joinToString("\n")

        return StringResult(
            headers = headers,
            value = value,
        )
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
        _: Database,
        _: AuthContext,
        transcoding: TranscodingCache,
    )
    fun getMediaStreamSegment(
        @PathParameter id: Uuid,
        @PathParameter name: String,
        @PathParameter segment: String,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
        @Header cookie: Cookie?,
        @Header range: String?,
    ): Result {
        val (item, session) = mediaSession(id, authorization, cookie, token)

        val headers = ParameterList("set-cookie" to "session=${session.jwt}; max-age=${session.maxAge}; path=/")

        val job = transcoding.job(item)
        val path = job.segment(name, segment)

        return stream(range, path, headers)
    }

    @Head("/stream/[id]/chapters.json")
    fun getMediaStreamChaptersHeaders() {
        val headers = ParameterList(
            "content-type" to "application/json",
        )

        throw NoContentSignal(headers)
    }

    @Get("/stream/[id]/chapters.json", result = "application/json")
    context(
        database: Database,
        _: AuthContext,
    )
    fun getMediaStreamChapters(
        @PathParameter id: Uuid,
        @QueryParameter token: String?,
        @Header authorization: Authorization?,
        @Header cookie: Cookie?,
    ): Result {
        val (item, session) = mediaSession(id, authorization, cookie, token)

        val headers = ParameterList("set-cookie" to "session=${session.jwt}; max-age=${session.maxAge}; path=/")

        val chapters = transaction(database) { item.chapters.toList() }

        val json = jsonOf(
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

        return StringResult(
            headers = headers,
            value = json.toString(),
        )
    }
}
