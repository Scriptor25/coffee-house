package dev.scriptor

import dev.scriptor.codec.AudioCodec
import dev.scriptor.codec.SubtitleCodec
import dev.scriptor.codec.VideoCodec
import dev.scriptor.model.media.*
import dev.scriptor.model.user.Session
import dev.scriptor.model.user.SessionTable
import dev.scriptor.model.user.UserTable
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.scan
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.time.Clock.System.now
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

fun Table.instant(name: String): Column<Instant> = registerColumn(
    name,
    object : IColumnType<Instant> {
        override var nullable: Boolean = false

        override fun sqlType(): String {
            return "TIMESTAMP"
        }

        override fun valueFromDB(value: Any): Instant? = when (value) {
            is Instant -> value
            is Timestamp -> value.toInstant().toKotlinInstant()
            is String -> Instant.parse(value)
            else -> error("unexpected value of type ${value::class}")
        }
    },
)

fun Table.path(name: String): Column<Path> = registerColumn(
    name,
    object : IColumnType<Path> {
        override var nullable: Boolean = false

        override fun sqlType(): String {
            return "TEXT"
        }

        override fun valueFromDB(value: Any): Path? = when (value) {
            is Path -> value
            is String -> Path(value)
            else -> error("unexpected value of type ${value::class}")
        }
    },
)

fun getEnvironment(): Map<String, String> = System.getenv()

val EXTENSIONS = arrayOf("mkv", "mp4", "webm")

fun parseFrameRate(value: String?): Double {
    if (value == null || value == "0/0") return 0.0

    val (num, den) = value
        .split("/")
        .map(String::toDouble)

    return if (den == 0.0) 0.0 else num / den
}

context(parent: Logger)
fun getMetadata(
    ffprobe: String,
    database: Database,
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

    val data = parseJson(json)

    val format = data["format"]
    val size = format["size"].get<String>().toLong()
    val duration = format["duration"].get<String>().toDouble()

    val tags = format["tags"]
    val title = tags["title"].get<String?>() ?: path.nameWithoutExtension

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

    val streams = data["streams"]
    for (stream in streams) {
        val codecType = stream["codec_type"].get<String>()

        when (codecType) {
            "video" -> {
                val index = stream["index"].get<Number>().toInt()
                val codec = stream["codec_name"].get<String>()
                val width = stream["width"].get<Number>().toInt()
                val height = stream["height"].get<Number>().toInt()
                val bitRate = stream["bit_rate"].get<String?>()?.toLongOrNull() ?: 0L
                val frameRate = parseFrameRate(stream["avg_frame_rate"].get<String?>())
                val profile = stream["profile"].get<String>()
                val level = stream["level"].get<Number>().toInt()

                val hdr = when (stream["color_transfer"].get<String?>()) {
                    "smpte2084", "arib-std-b67" -> true
                    else -> false
                }

                val tags = stream["tags"]
                val language = tags["language"].get<String?>()
                val title = tags["title"].get<String?>()

                val disposition = stream["disposition"]
                val default = disposition["default"].get<Number>() == 1

                transaction(database) {
                    VideoTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = codec
                        this.width = width
                        this.height = height
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
                val index = stream["index"].get<Number>().toInt()
                val codec = stream["codec_name"].get<String>()
                val bitRate = stream["bit_rate"].get<String?>()?.toLongOrNull() ?: 0L
                val sampleRate = stream["sample_rate"].get<String>().toLong()
                val channels = stream["channels"].get<Number>().toInt()

                val tags = stream["tags"]
                val language = tags["language"].get<String?>()
                val title = tags["title"].get<String?>()

                val disposition = stream["disposition"]
                val default = disposition["default"].get<Number>() == 1
                val forced = disposition["forced"].get<Number>() == 1

                transaction(database) {
                    AudioTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = codec
                        this.bitRate = bitRate
                        this.sampleRate = sampleRate
                        this.channels = channels
                        this.language = language
                        this.title = title
                        this.default = default
                        this.forced = forced
                    }
                }
            }

            "subtitle" -> {
                val index = stream["index"].get<Number>().toInt()
                val codec = stream["codec_name"].get<String>()

                val tags = stream["tags"]
                val language = tags["language"].get<String?>()
                val title = tags["title"].get<String?>()

                val disposition = stream["disposition"]
                val default = disposition["default"].get<Number>() == 1
                val forced = disposition["forced"].get<Number>() == 1

                transaction(database) {
                    SubtitleTrack.new {
                        this.media = media
                        this.index = index
                        this.codec = codec
                        this.language = language
                        this.title = title
                        this.default = default
                        this.forced = forced
                    }
                }
            }

            "attachment" -> {
                val index = stream["index"].get<Number>().toInt()
                val codec = stream["codec_name"].get<String?>()

                val tags = stream["tags"]
                val filename = tags["filename"].get<String?>()
                val mimetype = tags["mimetype"].get<String?>()

                // TODO: Attachment.new { ... }
            }
        }
    }

    val chapters = data["chapters"]

    var i = 0
    for (chapter in chapters) {
        val index = i++

        val start = chapter["start_time"].get<String>().toDouble()
        val end = chapter["end_time"].get<String>().toDouble()

        val tags = chapter["tags"]
        val language = tags["language"].get<String?>()
        val title = tags["title"].get<String?>()

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

fun main() {
    val env = getEnvironment()

    val hostname = env["HOSTNAME"] ?: "0.0.0.0"
    val port = env["PORT"]?.toInt() ?: 8080

    val data = Path(env["DATA"] ?: "/data")
    val cache = Path(env["CACHE"] ?: "/cache")

    val username = env["USERNAME"]
    val password = env["PASSWORD"]

    val transcodingEnable = env["TRANSCODING"].toBoolean()
    val transcodingDevice = env["TRANSCODING_DEVICE"]

    val preferredVideoCodec = env["PREFERRED_VIDEO_CODEC"]?.uppercase()
    val preferredAudioCodec = env["PREFERRED_AUDIO_CODEC"]?.uppercase()
    val preferredSubtitleCodec = env["PREFERRED_SUBTITLE_CODEC"]?.uppercase()

    val ffmpeg = env["FFMPEG"] ?: "ffmpeg"
    val ffprobe = env["FFPROBE"] ?: "ffprobe"

    val log = getLogger("coffee-house")
    log.level = Level.ALL

    val provider = Provider()

    provider["username"] = username
    provider["password"] = password

    provider.registerT(log)

    val capabilities = Probe(log, ffmpeg)()

    val databasePath = cache.resolve("index.db")
    databasePath.createParentDirectories()

    val database = Database.connect({ DriverManager.getConnection("jdbc:sqlite:$databasePath") })
    provider.registerT(database)

    val transcoding = TranscodingCache(
        ffmpeg,
        cache,
        capabilities,
        TranscodingRequirements(
            transcodingEnable,
            transcodingDevice,
            if (preferredVideoCodec != null)
                VideoCodec.valueOf(preferredVideoCodec)
            else VideoCodec.H264,
            if (preferredAudioCodec != null)
                AudioCodec.valueOf(preferredAudioCodec)
            else AudioCodec.AAC,
            if (preferredSubtitleCodec != null)
                SubtitleCodec.valueOf(preferredSubtitleCodec)
            else SubtitleCodec.WEBVTT,
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
        SchemaUtils.create(
            MediaTable,
            VideoTrackTable,
            AudioTrackTable,
            SubtitleTrackTable,
            ChapterTable,
            UserTable,
            SessionTable,
        )
    }

    transaction(database) {
        Media
            .find { MediaTable.path notInList paths }
            .forEach { it.delete() }
    }

    val existing = transaction {
        MediaTable
            .select(MediaTable.path)
            .map { it[MediaTable.path] }
            .toSet()
    }

    val revalidate = paths.filter { it !in existing }

    for ((index, path) in revalidate.withIndex()) {
        log.info("${index + 1} / ${revalidate.size}")

        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

        val createdAt = attributes.creationTime().toInstant().toKotlinInstant()
        val modifiedAt = attributes.lastModifiedTime().toInstant().toKotlinInstant()

        context(log) {
            getMetadata(
                ffprobe,
                database,
                path,
                createdAt,
                modifiedAt,
            )
        }
    }

    val server = Server(log, provider, hostname, port)

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

        server.register("session-reaper", 0L, 10L * 60L * 1000L) {
            transaction(database) {
                val now = now()
                Session
                    .find { SessionTable.expiresAt lessEq now }
                    .forEach { it.delete() }
            }
        }

        server.start()
    }
}
