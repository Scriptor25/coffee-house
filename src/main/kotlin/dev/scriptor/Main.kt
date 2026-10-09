package dev.scriptor

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.TranscodingTable
import dev.scriptor.model.ffmpeg.*
import dev.scriptor.model.media.*
import dev.scriptor.model.movie.MovieMediaTable
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.other.OtherTable
import dev.scriptor.model.show.*
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserRole
import dev.scriptor.model.user.UserTable
import dev.scriptor.security.Jwt
import dev.scriptor.server.Provider
import dev.scriptor.server.jvm.scan
import dev.scriptor.server.request.Request
import dev.scriptor.server.security.Authenticator
import dev.scriptor.server.security.Principal
import dev.scriptor.server.server
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import java.sql.DriverManager
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

data class HeaderAuthenticator(
    val log: Logger,
    val defaultUsername: String?,
    val defaultPassword: String?,
) : Authenticator {

    override fun authenticate(request: Request): Principal? {
        val authorization = request.headers["authorization"]
        val cookie = request.headers["cookie"]

        val token = when {
            authorization != null -> {
                val (scheme, credentials) = AuthorizationHeader.parse(authorization)

                if (scheme == "Bearer") credentials else null
            }

            cookie != null -> {
                val values = CookieHeader.parse(cookie)

                values["token"]
            }

            else -> null
        } ?: return null

        val jwt: Jwt
        try {
            jwt = Jwt.decode(token)
                ?: return null
        } catch (e: Throwable) {
            log.warning(e.stackTraceToString())
            return null
        }

        // TODO: change to something more secure
        if (!jwt.verify("hello-world-secret")) {
            return null
        }

        val instant = Clock.System.now()

        when (val exp = jwt.payload.exp) {
            null -> Unit
            else -> {
                val delta = exp - instant
                if (delta.isNegative()) {
                    return null
                }
            }
        }

        val id =
            when (val sub = jwt.payload.sub) {
                null -> Uuid.NIL
                else -> Uuid.parse(sub)
            }

        val role = if (id == Uuid.NIL) {
            UserRole.ADMIN
        } else {
            val user = db { User.findById(id) }
                ?: return null

            user.role
        }

        return Principal(id, setOf(role))
    }
}

fun getEnvironment(): Map<String, String> = System.getenv()

fun main() {
    val provider = Provider()
    scan(provider, "dev.scriptor")

    val env = getEnvironment()

    val host = env["HOST"]
    val port = env["PORT"]?.toInt()

    val data = Path(env["DATA"] ?: "/data")
    val cache = Path(env["CACHE"] ?: "/cache")

    val defaultUsername = env["USERNAME"]
    val defaultPassword = env["PASSWORD"]

    val transcodingEnable = env["TRANSCODING"].toBoolean()
    val transcodingDevice = env["TRANSCODING_DEVICE"]

    val targetVideoCodec = env["TARGET_VIDEO_CODEC"]?.lowercase() ?: "h264"
    val targetAudioCodec = env["TARGET_AUDIO_CODEC"]?.lowercase() ?: "aac"
    val targetSubtitleCodec = env["TARGET_SUBTITLE_CODEC"]?.lowercase() ?: "webvtt"

    val ffmpeg = env["FFMPEG"] ?: "ffmpeg"
    val ffprobe = env["FFPROBE"] ?: "ffprobe"

    val tmdbToken = env["TMDB_TOKEN"]

    provider["host"] = host
    provider["port"] = port

    provider["data"] = data
    provider["cache"] = cache

    provider["username"] = defaultUsername
    provider["password"] = defaultPassword

    provider["transcoding-enable"] = transcodingEnable
    provider["transcoding-device"] = transcodingDevice

    provider["target-video-codec"] = targetVideoCodec
    provider["target-audio-codec"] = targetAudioCodec
    provider["target-subtitle-codec"] = targetSubtitleCodec

    provider["ffmpeg"] = ffmpeg
    provider["ffprobe"] = ffprobe

    provider["tmdb-token"] = tmdbToken

    val log = getLogger("coffee-house")
    log.level = Level.ALL

    provider.setT(log)

    val databasePath = cache / "index.db"
    databasePath.createParentDirectories()

    val database = Database.connect({
        val connection = DriverManager.getConnection("jdbc:sqlite:$databasePath")
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        connection
    })
    provider.setT(database)

    val databaseThread = Thread { DatabaseController.run(database) }
    databaseThread.name = "database"
    databaseThread.isDaemon = true
    databaseThread.start()

    // TODO: drain database controller queue on shutdown

    db {
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

            ParentGroupTable,
            GroupTable,
            EpisodeGroupTable,

            OtherTable,

            TranscodingTable,
        )
    }

    Probe(log, ffmpeg, transcodingDevice)()

    val transcoding = TranscodingManager(
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
    provider.setT(transcoding)

    log.info("walk file tree")

    val nodes = walkFileTree(log, data)
    val paths = nodes.paths

    log.info("found ${paths.size} files (${nodes.movies.size} movies, ${nodes.shows.size} shows, ${nodes.others.size} others)")

    db {
        Media
            .find { MediaTable.path notInList paths }
            .forEach { it.delete() }
    }

    context(provider, log) {
        getFileMetadata(ffprobe, paths)
        getTmdbMetadata(nodes)
    }

    val server = server(log, provider) {
        when {
            host == null && port != null -> bind(port)
            host != null && port == null -> bind(host, 0)
            host != null && port != null -> bind(host, port)
            else -> Unit
        }

        authenticator = HeaderAuthenticator(
            log,
            defaultUsername,
            defaultPassword,
        )
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
            "cleanup-playbacks",
            Duration.ZERO,
            ofMinutes(60L).toKotlinDuration(),
        ) {
            val context: PlaybackContext = provider.getT()
                ?: error("missing playback context")
            context.cleanup()
        }

        server.register(
            "cleanup-transcoding",
            Duration.ZERO,
            ofHours(24L).toKotlinDuration()
        ) {
            val context: TranscodingManager = provider.getT()
                ?: error("missing transcoding manager")
            context.cleanup()
        }

        server.start()
    }
}
