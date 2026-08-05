package dev.scriptor

import dev.scriptor.context.SessionContext
import dev.scriptor.model.*
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.scan
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.reflect.full.starProjectedType
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

    val data = JSONObject(json)

    val format = data.getJSONObject("format")
    val size = format.getString("size").toLong()
    val duration = format.getString("duration").toDouble()
    val bitRate = format.getString("bit_rate").toLong()

    val tags = format.getJSONObject("tags")
    val title = tags.getString("title")

    val video = mutableListOf<VideoTrack>()
    val audio = mutableListOf<AudioTrack>()
    val subtitles = mutableListOf<SubtitleTrack>()

    val streams = data.getJSONArray("streams")
    for (i in 0 until streams.length()) {
        val stream = streams.getJSONObject(i)
        val codecType = stream.getString("codec_type")

        when (codecType) {
            "video" -> {
                val index = stream.getInt("index")
                val codec = stream.getString("codec_name")
                val width = stream.getInt("width")
                val height = stream.getInt("height")
                val frameRateStr = stream.getString("frame_rate")
                val profile = stream.getString("profile")
                val level = stream.getInt("level")
                val hdr = false // TODO
                val language = null // TODO
                val title = null // TODO

                val disposition = stream.getJSONObject("disposition")
                val default = disposition.getInt("default") == 1

                val frameRateParts = frameRateStr.split("/").map { it.toDouble() }
                val frameRate = frameRateParts[0] / frameRateParts[1]

                video += VideoTrack(
                    id,
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
                val index = stream.getInt("index")
                val codec = stream.getString("codec_name")
                val sampleRate = stream.getString("sample_rate").toLong()
                val channels = stream.getInt("channels")
                val language = null // TODO
                val title = null // TODO

                val disposition = stream.getJSONObject("disposition")
                val default = disposition.getInt("default") == 1

                audio += AudioTrack(
                    id,
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

    val chapters = data.getJSONArray("chapters")
    // TODO

    return Media(
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

    val hls = HlsCache(
        cache,
        transcoding,
    )

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
            SQL(connection).create<User>().execute()
            SQL(connection).create<Session>().execute()
            SQL(connection).create<Media>().execute()

            val entries = mutableListOf<Path>()

            SQL(connection)
                .insert<Media>()
                .conflict(Media::path) { it.updateExcluded(Media::id, Media::path) }
                .batch { submit ->
                    for (path in data.walk()) {
                        if (path.extension !in EXTENSIONS) continue

                        entries.add(path.absolute())

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

            SQL(connection)
                .delete()
                .from<Media>()
                .where(!(Media::path `in` entries))
                .execute()
        }

        server.register("session-reaper", 0L, 10L * 60L * 1000L) {
            val sessions = provider[SessionContext::class.starProjectedType] as SessionContext
            context(provider, connection) { sessions.deleteExpiredSessions() }
        }

        server.start()
    }

    connection.close()
}
