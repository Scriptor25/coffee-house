package dev.scriptor

import dev.scriptor.context.PlaybackContext
import dev.scriptor.context.TmdbContext
import dev.scriptor.model.ffmpeg.*
import dev.scriptor.model.media.*
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieMediaTable
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.show.*
import dev.scriptor.model.user.UserTable
import dev.scriptor.server.Provider
import dev.scriptor.server.http.Server
import dev.scriptor.server.jvm.scan
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toKotlinDuration
import kotlin.time.toKotlinInstant

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
    database: Database,
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
                val codec = stream.codecName?.lowercase()

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

context(
    _: Provider,
    log: Logger,
    database: Database,
)
fun getFileMetadata(
    ffprobe: String,
    paths: List<Path>,
) {
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

    for (result in results) {
        result.get()
    }
}

context(
    _: Provider,
    _: Logger,
    database: Database,
)
fun getTmdbMetadata(nodes: Nodes) {
    val context = TmdbContext()
    val tmdbIdRegex = """^[^\[]*\[tmdbid-(\d+)].*$""".toRegex()
    val seasonNumberRegex = """^.*(\d{1,2}).*$""".toRegex()
    val episodeNumberRegex = """^.*S(\d{1,2})E(\d{1,2}).*$""".toRegex()

    for (movieNode in nodes.movies) {
        val match = tmdbIdRegex.matchEntire(movieNode.path.name) ?: continue
        val movieId = match.groupValues[1].toInt()

        var movie = transaction(database) {
            Movie
                .find(MovieTable.tmdbId eq movieId)
                .firstOrNull()
        }

        if (movie == null) {
            val details = context.getMovieDetails(movieId) ?: continue

            movie = transaction(database) {
                Movie.new {
                    this.tmdbId = movieId
                    this.title = details.title
                    this.description = details.overview
                    this.poster = if (details.posterPath != null) "tmdb:${details.posterPath}" else null
                    this.backdrop = if (details.backdropPath != null) "tmdb:${details.backdropPath}" else null
                }
            }
        }

        transaction(database) {
            val items = movieNode.items
                .flatMap { Media.find(MediaTable.path eq it).toList() }
                .filter { it !in movie.items }
            val extra = movieNode.extra
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
    }

    for (showNode in nodes.shows) {
        val match = tmdbIdRegex.matchEntire(showNode.path.name) ?: continue
        val showId = match.groupValues[1].toInt()

        var show = transaction(database) {
            Show
                .find(ShowTable.tmdbId eq showId)
                .firstOrNull()
        }

        if (show == null) {
            val details = context.getShowDetails(showId) ?: continue

            show = transaction(database) {
                Show.new {
                    this.tmdbId = showId
                    this.title = details.name
                }
            }
        }

        for (seasonNode in showNode.seasons) {
            val match = seasonNumberRegex.matchEntire(seasonNode.path.name) ?: continue
            val seasonNumber = match.groupValues[1].toInt()

            var season = transaction(database) {
                Season
                    .find((SeasonTable.show eq show.id) and (SeasonTable.index eq seasonNumber))
                    .firstOrNull()
            }

            if (season == null) {
                val details = context.getSeasonDetails(showId, seasonNumber) ?: continue

                season = transaction(database) {
                    Season.new {
                        this.show = show
                        this.index = seasonNumber
                    }
                }
            }

            for (episodePath in seasonNode.episodes) {
                val match = episodeNumberRegex.matchEntire(episodePath.name) ?: continue
                val seasonNumber = match.groupValues[1].toInt()
                val episodeNumber = match.groupValues[2].toInt()

                var episode = transaction(database) {
                    Episode
                        .find((EpisodeTable.season eq season.id) and (EpisodeTable.index eq episodeNumber))
                        .firstOrNull()
                }

                if (episode == null) {
                    val details = context.getEpisodeDetails(showId, seasonNumber, episodeNumber) ?: continue

                    episode = transaction(database) {
                        Episode.new {
                            this.season = season
                            this.media = Media.find(MediaTable.path eq episodePath).first()
                            this.index = episodeNumber
                        }
                    }
                }
            }
        }
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
                        showNode = ShowNode(directory, showSeasons)
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

    val username = env["USERNAME"]
    val password = env["PASSWORD"]

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

    provider["username"] = username
    provider["password"] = password

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

    val database = Database.connect({ DriverManager.getConnection("jdbc:sqlite:$databasePath") })
    provider.setT(database)

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
    provider.setT(transcoding)

    log.info("walk file tree")

    val nodes = walkFileTree(log, data)
    val paths = nodes.paths

    log.info("found ${paths.size} files (${nodes.movies.size} movies, ${nodes.shows.size} shows, ${nodes.others.size} others)")

    transaction(database) {
        Media
            .find { MediaTable.path notInList paths }
            .forEach { it.delete() }
    }

    context(provider, log, database) {
        getFileMetadata(ffprobe, paths)
        getTmdbMetadata(nodes)
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
            val context: PlaybackContext = provider.getT()
                ?: error("missing playback context")
            context.deleteExpiredPlaybacks()
        }

        server.start()
    }
}
