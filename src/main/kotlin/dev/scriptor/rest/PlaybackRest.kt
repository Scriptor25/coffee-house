package dev.scriptor.rest

import dev.scriptor.JsonArrayNode
import dev.scriptor.TranscodingCache
import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.jsonOf
import dev.scriptor.model.CreatePlaybackBody
import dev.scriptor.model.RangeHeader
import dev.scriptor.model.media.Chapter
import dev.scriptor.model.media.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.ParameterList
import dev.scriptor.server.RangeNotSatisfiableSignal
import dev.scriptor.server.RangeReadableByteChannel
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.result.ChannelResult
import dev.scriptor.server.result.Result
import dev.scriptor.server.security.Principal
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.readText
import kotlin.io.path.useLines

@Controller("/resource/playback")
class PlaybackRest {

    context(context: PlaybackContext)
    private fun item(token: String, index: Int): Media {
        val playback = context.getPlayback(token)
            ?: throw NotFoundSignal()

        if (index !in playback.items.indices) {
            throw NotFoundSignal()
        }

        return db {
            Media.findById(playback.items[index].id)
        } ?: throw NotFoundSignal()
    }

    private fun stream(range: RangeHeader?, path: Path): Result {
        val channel = FileChannel.open(path)

        if (range == null) {
            return ChannelResult(value = channel)
        }

        val total = channel.size()

        val begin = range.begin
        val end = range.end ?: minOf(begin + 2L * 1024L * 1024L, total - 1L)

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

    @RequireAuth
    @Post("/", "application/json", "text/plain")
    context(
        principal: Principal,
        context: PlaybackContext,
    )
    fun createPlayback(@Body body: CreatePlaybackBody): String {
        val id = principal.id

        return context.createPlayback(id, body.name, body.items)
    }

    @Get("/[token]/playlist.m3u8", "application/x-mpegurl")
    context(context: PlaybackContext)
    fun getPlaylist(
        @PathParameter token: String,
        @QueryParameter direct: Boolean = false,
    ): String {
        val playback = context.getPlayback(token)
            ?: throw NotFoundSignal()

        val items = db {
            playback.items.mapNotNull {
                when (val value = Media.findById(it.id)) {
                    null -> null
                    else -> value to it.title
                }
            }
        }

        val lines = items.flatMapIndexed { index, item ->
            listOf(
                "#EXTINF:${item.first.duration},${item.second}",
                if (direct) "$index"
                else "$index/master.m3u8",
            )
        }

        return "#EXTM3U\r\n#PLAYLIST:${playback.name}\r\n${lines.joinToString("\r\n")}"
    }

    @Get("/[token]/[index]", "video/*")
    context(_: PlaybackContext)
    fun getStream(
        @PathParameter token: String,
        @PathParameter index: Int,
        @Header range: RangeHeader? = null,
    ): Result {
        val item = item(token, index)

        return stream(range, item.path)
    }

    @Get("/[token]/[index]/master.m3u8", "application/vnd.apple.mpegurl")
    context(
        _: Logger,
        _: PlaybackContext,
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
        _: PlaybackContext,
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
        _: PlaybackContext,
        transcoding: TranscodingCache,
    )
    fun getSegment(
        @PathParameter token: String,
        @PathParameter index: Int,
        @PathParameter name: String,
        @PathParameter segment: String,
        @Header range: RangeHeader? = null,
    ): Result {
        val item = item(token, index)

        val job = transcoding.job(item)
        val path = job.segment(name, segment)

        return stream(range, path)
    }

    @Get("/[token]/[index]/chapters.json", "application/json")
    context(_: PlaybackContext)
    fun getChapters(
        @PathParameter token: String,
        @PathParameter index: Int,
    ): JsonArrayNode {
        val item = item(token, index)

        val chapters = db { item.chapters.toList() }

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

