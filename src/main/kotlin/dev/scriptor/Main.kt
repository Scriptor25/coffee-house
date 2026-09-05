package dev.scriptor

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.ffmpeg.*
import dev.scriptor.model.media.*
import dev.scriptor.model.movie.MovieMediaTable
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.show.EpisodeMediaTable
import dev.scriptor.model.show.EpisodeTable
import dev.scriptor.model.show.SeasonTable
import dev.scriptor.model.show.ShowTable
import dev.scriptor.model.user.UserTable
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.jvm.scan
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.time.Duration.ofMinutes
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

fun getEnvironment(): Map<String, String> = System.getenv()

val EXTENSIONS = arrayOf("mkv", "mp4", "webm")

fun parseFrameRate(value: String?): Double {
    if (value == null || value == "0/0") return 0.0

    val (num, den) = value
        .split("/")
        .map(String::toDouble)

    return if (den == 0.0) 0.0 else num / den
}

data class FormatTagsNode(
    val title: String? = null,
)

data class FormatNode(
    val size: String,
    val duration: String,
    val tags: FormatTagsNode,
)

data class StreamTagsNode(
    val title: String? = null,
    val language: String? = null,
    val filename: String? = null,
    val mimetype: String? = null,
)

data class StreamDispositionNode(
    val default: Number? = null,
    val forced: Number? = null,
)

data class StreamNode(
    val index: Number,
    val codec_type: String,
    val codec_name: String? = null,
    val width: Number? = null,
    val height: Number? = null,
    val bit_rate: String? = null,
    val avg_frame_rate: String? = null,
    val profile: String? = null,
    val level: Number? = null,
    val color_transfer: String? = null,
    val sample_rate: String? = null,
    val channels: Number? = null,
    val tags: StreamTagsNode,
    val disposition: StreamDispositionNode,
)

data class ChapterTagsNode(
    val title: String? = null,
    val language: String? = null,
)

data class ChapterNode(
    val start_time: String,
    val end_time: String,
    val tags: ChapterTagsNode,
)

data class MetadataNode(
    val format: FormatNode,
    val streams: List<StreamNode>,
    val chapters: List<ChapterNode>,
)

context(
    log: Logger,
    database: Database,
)
fun getMetadata(
    ffprobe: String,
    path: Path,
    createdAt: Instant,
    modifiedAt: Instant,
) {
    val process = start(
        ffprobe,
        "-hide_banner",
        "-loglevel", "error",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        "-show_chapters",
        path.absolutePathString(),
    )

    val json = process.inputStream.reader().readText()

    val value = process.waitFor()
    if (value != 0) error("failed to get metadata for $path")

    val node: MetadataNode = parseJson(json).cast()

    val size = node.format.size.toLong()
    val duration = node.format.duration.toDouble()
    val title = node.format.tags.title ?: path.nameWithoutExtension

    val media = transaction(database) {
        Media.new {
            this.path = path
            this.size = size
            this.title = title
            this.createdAt = createdAt
            this.modifiedAt = modifiedAt
            this.duration = duration
        }
    }

    for (stream in node.streams) {
        val codecType = stream.codec_type.lowercase()

        when (codecType) {
            "video" -> {
                val index = stream.index.toInt()
                val codec = stream.codec_name?.lowercase()
                val width = stream.width?.toInt()
                val height = stream.height?.toInt()
                val bitRate = stream.bit_rate?.toLongOrNull() ?: 0L
                val frameRate = parseFrameRate(stream.avg_frame_rate)
                val profile = stream.profile
                val level = stream.level?.toInt()

                val hdr = when (stream.color_transfer) {
                    "smpte2084", "arib-std-b67" -> true
                    else -> false
                }

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1

                transaction(database) {
                    VideoTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = Codec[CodecId(codec!!)]
                        this.width = width!!
                        this.height = height!!
                        this.bitRate = if (bitRate == 0L)
                            size * 8L * 1000L / (duration * 1000.0).toLong()
                        else bitRate
                        this.frameRate = frameRate
                        this.profile = profile
                        this.level = level
                        this.hdr = hdr
                        this.language = language
                        this.title = title
                        this.default = default
                    }
                }
            }

            "audio" -> {
                val index = stream.index.toInt()
                val codec = stream.codec_name?.lowercase()
                val bitRate = stream.bit_rate?.toLongOrNull() ?: 0L
                val sampleRate = stream.sample_rate?.toLong()
                val channels = stream.channels?.toInt()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                transaction(database) {
                    AudioTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = Codec[CodecId(codec!!)]
                        this.bitRate = bitRate
                        this.sampleRate = sampleRate!!
                        this.channels = channels!!
                        this.language = language
                        this.title = title
                        this.default = default
                        this.forced = forced
                    }
                }
            }

            "subtitle" -> {
                val index = stream.index.toInt()
                val codec = stream.codec_name?.lowercase()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                transaction(database) {
                    SubtitleTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = Codec[CodecId(codec!!)]
                        this.language = language
                        this.title = title
                        this.default = default
                        this.forced = forced
                    }
                }
            }

            "attachment" -> {
                val index = stream.index.toInt()
                val codec = stream.codec_name?.lowercase()

                val filename = stream.tags.filename
                val mimetype = stream.tags.mimetype

                // TODO: Attachment.new { ... }
            }
        }
    }

    var i = 0
    for (chapter in node.chapters) {
        val index = i++

        val start = chapter.start_time.toDouble()
        val end = chapter.end_time.toDouble()

        val language = chapter.tags.language
        val title = chapter.tags.title

        transaction(database) {
            Chapter.new {
                this.media = media
                this.index = index
                this.start = start
                this.end = end
                this.language = language
                this.title = title
            }
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
context(
    log: Logger,
    database: Database,
)
fun getMetadata(
    ffprobe: String,
    paths: List<Path>,
) {
    while (true) {
        val existing = transaction(database) {
            MediaTable
                .select(MediaTable.path)
                .map { it[MediaTable.path] }
                .toSet()
        }

        val revalidate = paths.filterNot { it in existing }

        val executor = Executors.newFixedThreadPool(
            minOf(
                4,
                Runtime.getRuntime().availableProcessors(),
            ),
        )

        val index = AtomicInt(0)

        for (path in revalidate) {
            val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

            val createdAt = attributes.creationTime().toInstant().toKotlinInstant()
            val modifiedAt = attributes.lastModifiedTime().toInstant().toKotlinInstant()

            executor.execute {
                log.info("${index.incrementAndFetch()} / ${revalidate.size}")

                getMetadata(
                    ffprobe,
                    path,
                    createdAt,
                    modifiedAt,
                )
            }
        }

        executor.shutdown()
        if (executor.awaitTermination(60, TimeUnit.MINUTES)) break
    }
}

fun main() {
    val env = getEnvironment()

    val host = env["HOST"]
    val port = env["PORT"]?.toInt()

    val data = Path(env["DATA"] ?: "/data")
    val cache = Path(env["CACHE"] ?: "/cache")

    val username = env["USERNAME"]
    val password = env["PASSWORD"]

    val transcodingEnable = env["TRANSCODING"].toBoolean()
    val transcodingDevice = env["TRANSCODING_DEVICE"]

    val targetVideoCodec = env["TARGET_VIDEO_CODEC"]?.lowercase() ?: "h264"
    val targetAudioCodec = env["TARGET_AUDIO_CODEC"]?.lowercase() ?: "aac"
    val targetSubtitleCodec = env["TARGET_SUBTITLE_CODEC"]?.lowercase() ?: "webvtt"

    val ffmpeg = env["FFMPEG"] ?: "ffmpeg"
    val ffprobe = env["FFPROBE"] ?: "ffprobe"

    val log = getLogger("coffee-house")
    log.level = Level.ALL

    val provider = Provider()

    provider["username"] = username
    provider["password"] = password

    provider.registerT(log)

    val databasePath = cache.resolve("index.db")
    databasePath.createParentDirectories()

    val database = Database.connect({ DriverManager.getConnection("jdbc:sqlite:$databasePath") })
    provider.registerT(database)

    transaction(database) {
        SchemaUtils.create(
            DeviceTable,
            DeviceToDeviceTable,
            FormatTable,
            FilterTable,
            CodecTable,
            ImplementationTable,
            ImplementationDeviceTable,
            ImplementationFormatTable,

            MediaTable,
            VideoTrackTable,
            AudioTrackTable,
            SubtitleTrackTable,
            ChapterTable,

            UserTable,

            MovieTable,
            MovieMediaTable,

            ShowTable,
            SeasonTable,
            EpisodeTable,
            EpisodeMediaTable,
        )
    }

    Probe(log, ffmpeg, transcodingDevice)(database)

    val transcoding = TranscodingCache(
        log,
        ffmpeg,
        cache,
        TranscodingRequirements(
            transcodingEnable,
            transcodingDevice,
            CodecId(targetVideoCodec),
            CodecId(targetAudioCodec),
            CodecId(targetSubtitleCodec),
        ),
    )
    provider.registerT(transcoding)

    log.info("walking file tree")

    val paths = data
        .walk()
        .filter { it.extension in EXTENSIONS }
        .toList()

    log.info("found ${paths.size} files")

    transaction(database) {
        Media
            .find { MediaTable.path notInList paths }
            .forEach { it.delete() }
    }

    context(log, database) {
        getMetadata(ffprobe, paths)
    }

    val server = when {
        host == null && port == null -> Server(log, provider)
        host == null && port != null -> Server(log, provider, port)
        host != null && port == null -> Server(log, provider, host, 0)
        host != null && port != null -> Server(log, provider, host, port)
        else -> Server(log, provider)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            server.stop()
            server.close()
        } catch (e: Throwable) {
            log.warning(e.stackTraceToString())
        }
    })

    server.use { server ->
        scan(server, "dev.scriptor")

        server.register(
            "delete-expired-playbacks",
            Duration.ZERO,
            ofMinutes(60L).toKotlinDuration(),
        ) {
            val context: PlaybackContext = provider.getContextT()
                ?: error("missing playback context")
            context.deleteExpiredPlaybacks()
        }

        server.start()
    }
}
