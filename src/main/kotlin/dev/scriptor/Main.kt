package dev.scriptor

import dev.scriptor.context.PlaybackContext
import dev.scriptor.context.TmdbContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.ffmpeg.*
import dev.scriptor.model.media.*
import dev.scriptor.model.movie.ImageData
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieMediaTable
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.other.Other
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.sql.DriverManager
import java.time.Duration.ofMinutes
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.time.*
import kotlin.uuid.Uuid

fun getEnvironment(): Map<String, String> = System.getenv()

fun parseFrameRate(value: String?): Double {
    if (value == null || value == "0/0") return 0.0

    val (num, den) = value
        .split("/")
        .map(String::toDouble)

    return if (den == 0.0) 0.0 else num / den
}

@JsonSerializable
data class FormatTagsNode(
    @all:JsonProperty
    val title: String? = null,
)

@JsonSerializable
data class FormatNode(
    @all:JsonProperty
    val size: String,
    @all:JsonProperty
    val duration: String,
    @all:JsonProperty
    val tags: FormatTagsNode = FormatTagsNode(),
)

@JsonSerializable
data class StreamTagsNode(
    @all:JsonProperty
    val title: String? = null,
    @all:JsonProperty
    val language: String? = null,
    @all:JsonProperty
    val filename: String? = null,
    @all:JsonProperty
    val mimetype: String? = null,
)

@JsonSerializable
data class StreamDispositionNode(
    @all:JsonProperty
    val default: Number? = null,
    @all:JsonProperty
    val forced: Number? = null,
)

@JsonSerializable
data class StreamNode(
    @all:JsonProperty
    val index: Number,
    @all:JsonProperty("codec_type")
    val codecType: String,
    @all:JsonProperty("codec_name")
    val codecName: String? = null,
    @all:JsonProperty
    val width: Number? = null,
    @all:JsonProperty
    val height: Number? = null,
    @all:JsonProperty("bit_rate")
    val bitRate: String? = null,
    @all:JsonProperty("avg_frame_rate")
    val avgFrameRate: String? = null,
    @all:JsonProperty
    val profile: String? = null,
    @all:JsonProperty
    val level: Number? = null,
    @all:JsonProperty("sample_rate")
    val sampleRate: String? = null,
    @all:JsonProperty
    val channels: Number? = null,
    @all:JsonProperty
    val tags: StreamTagsNode = StreamTagsNode(),
    @all:JsonProperty
    val disposition: StreamDispositionNode = StreamDispositionNode(),
)

@JsonSerializable
data class ChapterTagsNode(
    @all:JsonProperty
    val title: String? = null,
    @all:JsonProperty
    val language: String? = null,
)

@JsonSerializable
data class ChapterNode(
    @all:JsonProperty("start_time")
    val startTime: String,
    @all:JsonProperty("end_time")
    val endTime: String,
    @all:JsonProperty
    val tags: ChapterTagsNode = ChapterTagsNode(),
)

@JsonSerializable
data class MetadataNode(
    @all:JsonProperty
    val format: FormatNode,
    @all:JsonProperty
    val streams: List<StreamNode>,
    @all:JsonProperty
    val chapters: List<ChapterNode>,
)

context(
    _: Provider,
    log: Logger,
)
fun getFileMetadata(
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

    val node: MetadataNode = parseJson(json).fromJson()

    val size = node.format.size.toLong()
    val duration = node.format.duration.toDouble()
    val title = node.format.tags.title ?: path.nameWithoutExtension

    val media = db {
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
        val codecType = stream.codecType.lowercase()

        when (codecType) {
            "video" -> {
                val index = stream.index.toInt()
                val codec = stream.codecName?.lowercase()
                val width = stream.width?.toInt()
                val height = stream.height?.toInt()
                val bitRate = stream.bitRate?.toLongOrNull() ?: 0L
                val frameRate = parseFrameRate(stream.avgFrameRate)
                val profile = stream.profile
                val level = stream.level?.toInt()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1

                db {
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
                        this.language = language
                        this.title = title
                        this.default = default
                    }
                }
            }

            "audio" -> {
                val index = stream.index.toInt()
                val codec = stream.codecName?.lowercase()
                val bitRate = stream.bitRate?.toLongOrNull() ?: 0L
                val sampleRate = stream.sampleRate?.toLong()
                val channels = stream.channels?.toInt()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                db {
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
                val codec = stream.codecName?.lowercase()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                db {
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
                val codec = stream.codecName?.lowercase()

                val filename = stream.tags.filename
                val mimetype = stream.tags.mimetype

                // TODO: Attachment.new { ... }
            }
        }
    }

    var i = 0
    for (chapter in node.chapters) {
        val index = i++

        val start = chapter.startTime.toDouble()
        val end = chapter.endTime.toDouble()

        val language = chapter.tags.language
        val title = chapter.tags.title

        db {
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

context(
    _: Provider,
    log: Logger,
)
fun getFileMetadata(
    ffprobe: String,
    paths: List<Path>,
) {
    val existing = db {
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

    val tasks = mutableListOf<Callable<Unit>>()

    for ((index, path) in revalidate.withIndex()) {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

        val createdAt = attributes.creationTime().toInstant().toKotlinInstant()
        val modifiedAt = attributes.lastModifiedTime().toInstant().toKotlinInstant()

        tasks.add {
            log.info("${index + 1} / ${revalidate.size}")

            getFileMetadata(
                ffprobe,
                path,
                createdAt,
                modifiedAt,
            )
        }
    }

    val results = executor.invokeAll(tasks)

    executor.shutdown()

    for (result in results) {
        result.get()
    }
}

enum class TmdbImageType {
    BACKDROP,
    LOGO,
    POSTER,
    PROFILE,
    STILL,
}

fun buildTmdbImages(configuration: TmdbContext.Configuration, type: TmdbImageType, path: String): List<ImageData> {
    val base = configuration.images.secureBaseUrl

    val sizes = when (type) {
        TmdbImageType.BACKDROP -> configuration.images.backdropSizes
        TmdbImageType.LOGO -> configuration.images.logoSizes
        TmdbImageType.POSTER -> configuration.images.posterSizes
        TmdbImageType.PROFILE -> configuration.images.profileSizes
        TmdbImageType.STILL -> configuration.images.stillSizes
    }

    return sizes.map {
        val url = "$base$it$path"
        val width = when (it) {
            "original" -> -1
            else -> it.slice(1 until it.length).toInt()
        }

        ImageData(url, width)
    }
}

val TMDB_ID_REGEX = """^[^\[]*\[tmdbid-(\d+)].*$""".toRegex()
val SEASON_NUMBER_REGEX = """^.*(\d{1,2}).*$""".toRegex()
val EPISODE_NUMBER_REGEX = """^.*S(\d{1,2})E(\d{1,2}).*$""".toRegex()

context(
    _: Logger,
    _: Provider,
)
fun getMovieMetadata(
    context: TmdbContext,
    configuration: TmdbContext.Configuration,
    node: MovieNode,
): Movie {
    when (val movie = db {
        Movie
            .find(MovieTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return movie
    }

    val match = TMDB_ID_REGEX.matchEntire(node.path.name)
    val movieId = match?.let { it.groupValues[1].toInt() }

    val details = if (movieId != null)
        context.getMovieDetails(movieId)
    else null

    val movie = db {
        Movie.new {
            this.path = node.path
            this.tmdbId = movieId

            if (details != null) {
                this.title = details.title
                this.description = details.overview
                this.poster = when (val path = details.posterPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.POSTER, path)
                }
                this.backdrop = when (val path = details.backdropPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.BACKDROP, path)
                }
            } else {
                this.title = node.path.name
                this.description = null
                this.poster = emptyList()
                this.backdrop = emptyList()
            }
        }
    }

    db {
        val items = node.items
            .flatMap { Media.find(MediaTable.path eq it).toList() }
            .filter { it !in movie.items }
        val extra = node.extra
            .flatMap { Media.find(MediaTable.path eq it).toList() }
            .filter { it !in movie.items }

        MovieMediaTable.batchInsert(items) { media ->
            this[MovieMediaTable.movie] = movie.id
            this[MovieMediaTable.media] = media.id
            this[MovieMediaTable.extra] = false
        }
        MovieMediaTable.batchInsert(extra) { media ->
            this[MovieMediaTable.movie] = movie.id
            this[MovieMediaTable.media] = media.id
            this[MovieMediaTable.extra] = true
        }
    }

    return movie
}

context(
    _: Logger,
    _: Provider,
)
fun getEpisodeMetadata(
    context: TmdbContext,
    configuration: TmdbContext.Configuration,
    show: Show,
    season: Season,
    index: Int,
    path: Path,
): Episode {
    when (val episode = db {
        Episode
            .find(EpisodeTable.path eq path)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return episode
    }

    val match = EPISODE_NUMBER_REGEX.matchEntire(path.name)
    val seasonNumber = match?.let { it.groupValues[1].toInt() }
    val episodeNumber = match?.let { it.groupValues[2].toInt() }

    val details = if (seasonNumber != null && episodeNumber != null) {
        context.getEpisodeDetails(show.tmdbId ?: 0, seasonNumber, episodeNumber)
    } else null

    return db {
        Episode.new {
            this.path = path
            this.season = season
            this.media = Media.find(MediaTable.path eq path).first()

            if (details != null) {
                this.index = details.episodeNumber
                this.title = details.name
                this.description = details.overview
                this.still = when (val path = details.stillPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.STILL, path)
                }
            } else {
                this.index = episodeNumber ?: index
                this.title = media.title
                this.description = null
                this.still = emptyList()
            }
        }
    }
}

context(
    _: Logger,
    _: Provider,
)
fun getSeasonMetadata(
    context: TmdbContext,
    configuration: TmdbContext.Configuration,
    show: Show,
    index: Int,
    node: SeasonNode,
): Season {
    when (val season = db {
        Season
            .find(SeasonTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return season
    }

    val match = SEASON_NUMBER_REGEX.matchEntire(node.path.name)
    val seasonNumber = match?.let { it.groupValues[1].toInt() }

    val details = if (seasonNumber != null) {
        context.getSeasonDetails(show.tmdbId ?: 0, seasonNumber)
    } else null

    val season = db {
        Season.new {
            this.path = node.path
            this.show = show

            if (details != null) {
                this.index = details.seasonNumber
                this.title = details.name
                this.description = details.overview
                this.poster = when (val path = details.posterPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.POSTER, path)
                }
            } else {
                this.index = seasonNumber ?: index
                this.title = node.path.name
                this.description = null
                this.poster = emptyList()
            }
        }
    }

    for ((index, path) in node.episodes.withIndex()) {
        getEpisodeMetadata(
            context,
            configuration,
            show,
            season,
            index,
            path,
        )
    }

    return season
}

context(
    _: Logger,
    _: Provider,
)
fun getShowMetadata(
    context: TmdbContext,
    configuration: TmdbContext.Configuration,
    node: ShowNode,
): Show {
    when (val show = db {
        Show
            .find(ShowTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return show
    }

    val match = TMDB_ID_REGEX.matchEntire(node.path.name)
    val showId = match?.let { it.groupValues[1].toInt() }

    val details = if (showId != null)
        context.getShowDetails(showId)
    else null

    val show = db {
        Show.new {
            this.path = node.path
            this.tmdbId = showId

            if (details != null) {
                this.title = details.name
                this.description = details.overview
                this.poster = when (val path = details.posterPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.POSTER, path)
                }
                this.backdrop = when (val path = details.backdropPath) {
                    null -> listOf()
                    else -> buildTmdbImages(configuration, TmdbImageType.BACKDROP, path)
                }
            } else {
                this.title = node.path.name
                this.description = null
                this.poster = emptyList()
                this.backdrop = emptyList()
            }
        }
    }

    for ((index, node) in node.seasons.withIndex()) {
        getSeasonMetadata(
            context,
            configuration,
            show,
            index,
            node,
        )
    }

    return show
}

fun getOtherMetadata(path: Path): Other {
    val media = db {
        Media
            .find(MediaTable.path eq path)
            .first()
    }

    when (val other = db {
        Other
            .find(OtherTable.media eq media.id)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return other
    }

    return db {
        Other.new {
            this.media = media
            this.title = media.title
        }
    }
}

context(
    _: Provider,
    _: Logger,
)
fun getTmdbMetadata(nodes: Nodes) {
    val context = TmdbContext()
    val configuration = context.getConfiguration()
        ?: error("failed to get tmdb configuration")

    db {
        Movie.all().filter { it.items.empty() }.forEach { it.delete() }
        Season.all().filter { it.episodes.empty() }.forEach { it.delete() }
        Show.all().filter { it.seasons.empty() }.forEach { it.delete() }
    }

    for (node in nodes.movies) {
        getMovieMetadata(
            context,
            configuration,
            node,
        )
    }

    for (node in nodes.shows) {
        getShowMetadata(
            context,
            configuration,
            node,
        )
    }

    for (path in nodes.others) {
        getOtherMetadata(path)
    }
}

data class MovieNode(
    val path: Path,
    val items: List<Path>,
    val extra: List<Path>,
)

data class SeasonNode(
    val path: Path,
    val episodes: List<Path>,
)

data class ShowNode(
    val path: Path,
    val seasons: List<SeasonNode>,
)

data class Nodes(
    val movies: List<MovieNode>,
    val shows: List<ShowNode>,
    val others: List<Path>,
) {
    val paths: List<Path>
        get() = movies.flatMap { movie -> movie.items + movie.extra } +
                shows.flatMap { show -> show.seasons.flatMap { season -> season.episodes } } +
                others
}

fun walkFileTree(log: Logger, root: Path): Nodes {
    var moviesPath: Path? = null
    var moviePath: Path? = null
    var extraPath: Path? = null

    var showsPath: Path? = null
    var showPath: Path? = null
    var seasonPath: Path? = null

    lateinit var movieNode: MovieNode
    lateinit var movieItems: MutableList<Path>
    lateinit var movieExtra: MutableList<Path>

    lateinit var showNode: ShowNode
    lateinit var showSeasons: MutableList<SeasonNode>
    lateinit var seasonNode: SeasonNode
    lateinit var seasonEpisodes: MutableList<Path>

    val movies = mutableListOf<MovieNode>()
    val shows = mutableListOf<ShowNode>()
    val others = mutableListOf<Path>()

    root.visitFileTree {
        onPreVisitDirectory { directory, attributes ->
            when {
                moviesPath != null -> when {
                    moviePath == null -> {
                        moviePath = directory
                        movieItems = mutableListOf()
                        movieExtra = mutableListOf()
                        movieNode = MovieNode(directory, movieItems, movieExtra)
                        FileVisitResult.CONTINUE
                    }

                    extraPath == null -> {
                        extraPath = directory
                        FileVisitResult.CONTINUE
                    }

                    else -> FileVisitResult.CONTINUE
                }

                showsPath != null -> when {
                    showPath == null -> {
                        showPath = directory
                        showSeasons = mutableListOf()
                        showNode = ShowNode(
                            directory,
                            showSeasons,
                        )
                        FileVisitResult.CONTINUE
                    }

                    seasonPath == null -> {
                        seasonPath = directory
                        seasonEpisodes = mutableListOf()
                        seasonNode = SeasonNode(directory, seasonEpisodes)
                        FileVisitResult.CONTINUE
                    }

                    else -> FileVisitResult.CONTINUE
                }

                directory.name.equals("movies", true) -> {
                    moviesPath = directory
                    FileVisitResult.CONTINUE
                }

                directory.name.equals("shows", true) -> {
                    showsPath = directory
                    FileVisitResult.CONTINUE
                }

                else -> FileVisitResult.CONTINUE
            }
        }

        onPostVisitDirectory { directory, exception ->
            if (exception != null) {
                log.warning("error when visiting directory $directory: ${exception.stackTraceToString()}")
            }

            when (directory) {
                moviesPath -> moviesPath = null

                moviePath -> {
                    movies.add(movieNode)
                    moviePath = null
                }

                extraPath -> extraPath = null

                showsPath -> showsPath = null

                showPath -> {
                    shows.add(showNode)
                    showPath = null
                }

                seasonPath -> {
                    showSeasons.add(seasonNode)
                    seasonPath = null
                }
            }

            FileVisitResult.CONTINUE
        }

        onVisitFile { file, attributes ->
            when {
                extraPath != null -> movieExtra.add(file)
                moviePath != null -> movieItems.add(file)

                seasonPath != null -> seasonEpisodes.add(file)

                else -> others.add(file)
            }

            FileVisitResult.CONTINUE
        }

        onVisitFileFailed { file, exception ->
            log.warning("error when visiting file $file: ${exception.stackTraceToString()}")

            FileVisitResult.CONTINUE
        }
    }

    return Nodes(
        movies,
        shows,
        others,
    )
}

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

    val databasePath = cache.resolve("index.db")
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

            OtherTable,
        )
    }

    Probe(log, ffmpeg, transcodingDevice)()

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

    context(provider, log, database) {
        getFileMetadata(ffprobe, paths)
        getTmdbMetadata(nodes)
    }

    val server = server(log, provider) {
        when {
            host == null && port != null -> bind(port)
            host != null && port == null -> bind(host, 0)
            host != null && port != null -> bind(host, port)
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
            "delete-expired-playbacks",
            Duration.ZERO,
            ofMinutes(60L).toKotlinDuration(),
        ) {
            val context: PlaybackContext = provider.getT()
                ?: error("missing playback context")
            context.deleteExpiredPlaybacks()
        }

        server.start()
    }
}

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
                else -> Uuid.parseHexDash(sub)
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
