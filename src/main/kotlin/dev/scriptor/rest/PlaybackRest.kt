package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.TranscodingCache
import dev.scriptor.context.AuthContext
import dev.scriptor.get
import dev.scriptor.jsonOf
import dev.scriptor.model.Authorization
import dev.scriptor.model.media.Chapter
import dev.scriptor.model.media.Media
import dev.scriptor.server.*
import dev.scriptor.server.annotation.*
import dev.scriptor.server.result.ChannelResult
import dev.scriptor.server.result.Result
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.security.SecureRandom
import java.time.Duration.ofHours
import java.util.logging.Logger
import kotlin.io.encoding.Base64
import kotlin.io.path.readText
import kotlin.io.path.useLines
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/playback")
class PlaybackRest {

    private data class Playback(
        val userId: Uuid?,
        val name: String,
        val items: List<Uuid>,
        val createdAt: Instant,
        val expiresAt: Instant,
    )

    private val random = SecureRandom()
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    private val map = mutableMapOf<String, Playback>()

    private fun playback(token: String): Playback {
        val playback = map[token]
            ?: throw UnauthorizedSignal()

        val delta = playback.expiresAt - Clock.System.now()
        if (delta.isNegative()) {
            map.remove(token)
            throw UnauthorizedSignal()
        }

        return playback
    }

    context(database: Database)
    private fun item(token: String, index: Int): Media {
        val playback = playback(token)

        if (index !in playback.items.indices) {
            throw NotFoundSignal()
        }

        return transaction(database) {
            Media.findById(playback.items[index])
        } ?: throw NotFoundSignal()
    }

    private fun stream(range: String?, path: Path): Result {
        val channel = FileChannel.open(path)

        if (range.isNullOrBlank()) {
            return ChannelResult(value = channel)
        }

        val total = channel.size()

        val range = range
            .substringAfter("bytes=")
            .split("-", limit = 2)
            .filter { it.isNotBlank() }

        val begin = range[0].toLong()
        val end = if (range.size == 2) range[1].toLong() else (total - 1L)

        val headers = ParameterList()

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

    @Post("/", "application/json", "text/plain")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun createPlayback(
        @Header authorization: Authorization,
        @Body node: JsonNode,
    ): String {
        val createdAt = Clock.System.now()
        val expiresAt = createdAt + ofHours(24).toKotlinDuration()

        val session = auth.auth(authorization, createdAt)
            ?: throw UnauthorizedSignal()

        val userId = session.user?.id?.value

        val nameNode = node["name"]
        val itemsNode = node["items"]

        val name = nameNode.get<String>()
        val items = itemsNode.map { Uuid.parseHexDash(it.get()) }

        while (true) {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)

            val token = base64.encode(bytes)
            if (token in map) continue

            map[token] = Playback(
                userId,
                name,
                items,
                createdAt,
                expiresAt,
            )

            return token
        }
    }

    @Get("/[token]/playlist.m3u8", "application/x-mpegurl")
    context(database: Database)
    fun getPlaylist(
        @PathParameter token: String,
        @QueryParameter direct: Boolean?,
    ): String {
        val direct = direct ?: false

        val playback = playback(token)

        val items = transaction(database) {
            playback.items.map { Media.findById(it) }
        }

        val lines = items
            .mapIndexedNotNull { index, item ->
                if (item == null) null
                else listOf(
                    "#EXTINF:${item.duration},${item.title}",
                    if (direct) "$index"
                    else "$index/master.m3u8",
                )
            }
            .flatten()

        return "#EXTM3U\r\n#PLAYLIST:${playback.name}\r\n${lines.joinToString("\r\n")}"
    }

    @Get("/[token]/[index]", "video/*")
    context(_: Database)
    fun getStream(
        @PathParameter token: String,
        @PathParameter index: Int,
        @Header range: String?,
    ): Result {
        val item = item(token, index)

        return stream(range, item.path)
    }

    @Get("/[token]/[index]/master.m3u8", "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
    )
    fun getMaster(
        @PathParameter token: String,
        @PathParameter index: Int,
    ): String {
        val item = item(token, index)

        val job = transcoding.job(item)
        val path = job.master()

        val manifest = path.useLines {
            (it + """#EXT-X-SESSION-DATA:DATA-ID="com.apple.hls.chapters",URI="chapters.json"""")
                .joinToString("\n")
        }

        return manifest
    }

    @Get("/[token]/[index]/[name]/index.m3u8", "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
    )
    fun getIndex(
        @PathParameter token: String,
        @PathParameter index: Int,
        @PathParameter name: String,
    ): String {
        val item = item(token, index)

        val job = transcoding.job(item)
        val path = job.index(name)

        return path.readText()
    }

    @Get("/[token]/[index]/[name]/[segment].mp4", "video/mp4")
    context(
        _: Logger,
        database: Database,
        transcoding: TranscodingCache,
    )
    fun getSegment(
        @PathParameter token: String,
        @PathParameter index: Int,
        @PathParameter name: String,
        @PathParameter segment: String,
        @Header range: String?,
    ): Result {
        val item = item(token, index)

        val job = transcoding.job(item)
        val path = job.segment(name, segment)

        return stream(range, path)
    }

    @Get("/[token]/[index]/chapters.json", "application/json")
    context(
        database: Database,
    )
    fun getChapters(
        @PathParameter token: String,
        @PathParameter index: Int,
    ): JsonNode {
        val item = item(token, index)

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
