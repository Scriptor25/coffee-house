package dev.scriptor

import dev.scriptor.context.SessionContext
import dev.scriptor.model.Media
import dev.scriptor.model.Metadata
import dev.scriptor.model.Session
import dev.scriptor.model.User
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.scan
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVRational
import org.bytedeco.ffmpeg.global.avcodec.avcodec_get_name
import org.bytedeco.ffmpeg.global.avformat.*
import org.bytedeco.ffmpeg.global.avutil.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.util.logging.Level
import kotlin.io.path.*
import kotlin.reflect.typeOf
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

fun getEnvironment(): Map<String, String> = System.getenv()

val EXTENSIONS = arrayOf("mkv", "mp4")

fun getMetadata(id: Uuid, path: Path): Metadata {
    val context = avformat_alloc_context()

    if (avformat_open_input(context, path.absolutePathString(), null, null) < 0) {
        error("failed to open file $path")
    }

    try {
        if (avformat_find_stream_info(context, null as AVDictionary?) < 0) {
            error("failed to read stream information")
        }

        var width = 0
        var height = 0
        var fps = 0.0
        var videoCodec: String? = null
        var audioCodec: String? = null

        for (i in 0 until context.nb_streams()) {
            val stream = context.streams(i)
            val codec = stream.codecpar()

            when (codec.codec_type()) {
                AVMEDIA_TYPE_VIDEO -> {
                    width = codec.width()
                    height = codec.height()

                    val rate: AVRational = stream.avg_frame_rate()
                    if (rate.den() != 0) {
                        fps = rate.num().toDouble() / rate.den()
                    }

                    videoCodec = avcodec_get_name(codec.codec_id()).string
                }

                AVMEDIA_TYPE_AUDIO -> {
                    audioCodec = avcodec_get_name(codec.codec_id()).string
                }
            }
        }

        return Metadata(
            id,

            context.duration() * 1000L / AV_TIME_BASE,
            context.bit_rate(),

            width,
            height,

            fps,

            videoCodec,
            audioCodec,
        )
    } finally {
        avformat_close_input(context)
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

        context(provider) {
            SQL(connection).create<User>().execute()
            SQL(connection).create<Session>().execute()
            SQL(connection).create<Media>().execute()
            SQL(connection).create<Metadata>().execute()

            val entries = mutableListOf<Path>()

            SQL(connection)
                .insert<Media>()
                .conflict(Media::path) { sql ->
                    sql.update(
                        Media::size to excluded("size"),
                        Media::title to excluded("title"),
                        Media::createdAt to excluded("created_at"),
                        Media::modifiedAt to excluded("modified_at"),
                    )
                }
                .batch { submit ->
                    for (path in data.walk()) {
                        if (path.extension !in EXTENSIONS) continue

                        entries.add(path.absolute())

                        val id = Uuid.random()

                        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

                        val size = attributes.size()

                        val createdAt = attributes.creationTime()
                        val modifiedAt = attributes.lastModifiedTime()

                        submit(
                            Media(
                                id,
                                path,
                                size,
                                path.nameWithoutExtension,
                                createdAt.toInstant().toKotlinInstant(),
                                modifiedAt.toInstant().toKotlinInstant(),
                            ),
                        )
                    }
                }

            SQL(connection)
                .delete()
                .from<Media>()
                .where(!(Media::path `in` entries))
                .execute()

            val items = SQL(connection)
                .selectFrom<Media>()
                .query<Media>()

            SQL(connection)
                .insert<Metadata>()
                .conflict(Metadata::id) { sql ->
                    sql.update(
                        Metadata::duration to excluded("duration"),
                        Metadata::bitrate to excluded("bitrate"),
                        Metadata::width to excluded("width"),
                        Metadata::height to excluded("height"),
                        Metadata::framerate to excluded("framerate"),
                        Metadata::videoCodec to excluded("video_codec"),
                        Metadata::audioCodec to excluded("audio_codec"),
                    )
                }
                .batch { submit ->
                    for ((id, path) in items) {
                        log.info("get metadata for $id : $path")

                        val metadata = getMetadata(id, path)

                        submit(metadata)
                    }
                }
        }

        server.register("session-reaper", 0L, 10L * 60L * 1000L) {
            val sessions = provider[typeOf<SessionContext>()] as SessionContext
            context(provider, connection) { sessions.deleteExpiredSessions() }
        }

        server.start()
    }

    connection.close()
}
