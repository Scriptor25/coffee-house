package dev.scriptor

import dev.scriptor.ConflictMode.REPLACE
import dev.scriptor.context.SessionContext
import dev.scriptor.model.*
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.scan
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.sql.JDBCType.TIMESTAMP
import java.sql.JDBCType.VARCHAR
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun getEnvironment(): Map<String, String> = System.getenv()

val EXTENSIONS = arrayOf("mkv", "mp4")

context(parent: Logger)
fun getMetadata(
    id: Uuid,
    path: Path,
    createdAt: Instant,
    modifiedAt: Instant,
): Media {

    val command = listOf(
        "ffprobe",
        "-v", "quiet",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        "-show_chapters",
        path.absolutePathString(),
    )

    val log = getLogger("ffprobe", parent)

    log.fine(command.joinToString("' '", "'", "'"))

    val process = ProcessBuilder(command).start()

    process.attach(log)

    val value = process.waitFor()
    if (value != 0) error("failed to get metadata for $path")

    val json = process.inputStream.reader().readText()

    log.info(json)

    val data = parseJson(json)

    val format = data["format"]
    val size = format["size"].get<String>().toLong()
    val duration = format["duration"].get<String>().toDouble()
    val bitRate = format["bit_rate"].get<String>().toLong()

    val tags = format["tags"]
    val title = tags["title"].get<String>()

    val video = mutableListOf<VideoTrack>()
    val audio = mutableListOf<AudioTrack>()
    val subtitles = mutableListOf<SubtitleTrack>()

    val media = Media(
        id,
        path,
        size,
        title,
        createdAt,
        modifiedAt,
        duration,
        video,
        audio,
        subtitles,
    )

    val streams = data["streams"]
    for (stream in streams) {
        val codecType = stream["codec_type"].get<String>()

        when (codecType) {
            "video" -> {
                val index = stream["index"].get<Int>()
                val codec = stream["codec_name"].get<String>()
                val width = stream["width"].get<Int>()
                val height = stream["height"].get<Int>()
                val frameRateStr = stream["frame_rate"].get<String?>()?.ifEmpty { "0/1" } ?: "0/1"
                val profile = stream["profile"].get<String>()
                val level = stream["level"].get<Int>()
                val hdr = false // TODO
                val language = null // TODO
                val title = null // TODO

                val disposition = stream["disposition"]
                val default = disposition["default"].get<Int>() == 1

                val frameRateParts = frameRateStr.split("/").map { it.toDouble() }
                val frameRate = frameRateParts[0] / frameRateParts[1]

                video += VideoTrack(
                    id,
                    media,
                    index,
                    codec,
                    width,
                    height,
                    bitRate,
                    frameRate,
                    profile,
                    level,
                    hdr,
                    language,
                    title,
                    default,
                )
            }

            "audio" -> {
                val index = stream["index"].get<Int>()
                val codec = stream["codec_name"].get<String>()
                val sampleRate = stream["sample_rate"].get<String>().toLong()
                val channels = stream["channels"].get<Int>()
                val language = null // TODO
                val title = null // TODO

                val disposition = stream["disposition"]
                val default = disposition["default"].get<Int>() == 1

                audio += AudioTrack(
                    id,
                    media,
                    index,
                    codec,
                    bitRate,
                    sampleRate,
                    channels,
                    language,
                    title,
                    default,
                )
            }

            "subtitle" -> {
                // TODO
            }

            "attachment" -> {
                // TODO
            }
        }
    }

    // TODO
    val chapters = data["chapters"]

    return media
}

@OptIn(ExperimentalUuidApi::class)
fun main() {
    val env = getEnvironment()

    val hostname = env["HOSTNAME"] ?: "0.0.0.0"
    val port = env["PORT"]?.toInt() ?: 8080
    val data = Path(env["DATA"] ?: "/data")
    val cache = Path(env["CACHE"] ?: "/cache")
    val username = env["USERNAME"]
    val password = env["PASSWORD"]
    val transcoding = env["TRANSCODING"].toBoolean()

    val provider = Provider()

    provider["hostname"] = hostname
    provider["port"] = port
    provider["data"] = data
    provider["cache"] = cache
    provider["username"] = username
    provider["password"] = password
    provider["transcoding"] = transcoding

    val log = getLogger("coffee-house")
    log.level = Level.ALL
    provider += log

    val db = cache.resolve("index.db")
    db.createParentDirectories()

    val connection = DriverManager.getConnection("jdbc:sqlite:$db")
    provider += connection

    val entities = EntityConnection(
        provider,
        connection,
        mapOf(
            typeOf<Uuid>() to VARCHAR,
            typeOf<Path>() to VARCHAR,
            typeOf<UserRole>() to VARCHAR,
            typeOf<Instant>() to TIMESTAMP,
        ),
    )
    provider += entities

    val hls = HlsCache(cache, transcoding)
    provider += hls

    val server = Server(log, provider, hostname, port)

    Runtime.getRuntime().addShutdownHook(Thread {
        try {
            server.stop()
            server.close()
        } catch (e: Throwable) {
            log.warning(e.stackTraceToString())
        }

        connection.close()
    })

    server.use { server ->
        scan(server, "dev.scriptor")

        context(log, provider) {
            entities.createTable<User>()
            entities.createTable<Session>()
            entities.createTable<Media>()
            entities.createTable<VideoTrack>()
            entities.createTable<AudioTrack>()
            entities.createTable<SubtitleTrack>()

            val paths = mutableListOf<Path>()

            entities.create(REPLACE) { submit ->
                for (path in data.walk()) {
                    if (path.extension !in EXTENSIONS) continue

                    paths.add(path.absolute())

                    val id = Uuid.generateV7()

                    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

                    val createdAt = attributes.creationTime()
                    val modifiedAt = attributes.lastModifiedTime()

                    val media = getMetadata(
                        id,
                        path,
                        createdAt.toInstant().toKotlinInstant(),
                        modifiedAt.toInstant().toKotlinInstant(),
                    )

                    submit(media)
                }
            }

            entities.deleteAll<Media>(!("path" `in` paths))
        }

        server.register("session-reaper", 0L, 10L * 60L * 1000L) {
            val sessions = provider[SessionContext::class.starProjectedType] as SessionContext
            context(provider, entities) { sessions.deleteExpiredSessions() }
        }

        server.start()
    }

    connection.close()
}
