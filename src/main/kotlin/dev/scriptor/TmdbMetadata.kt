package dev.scriptor

import dev.scriptor.context.TmdbContext
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.MediaTable
import dev.scriptor.model.movie.ImageData
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieMediaTable
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.model.other.Other
import dev.scriptor.model.other.OtherTable
import dev.scriptor.model.show.*
import dev.scriptor.server.Provider
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.name

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
val EPISODE_NUMBER_REGEX = """^.*[Ss](\d{1,2})[Ee](\d{1,2}).*$""".toRegex()

context(
    _: Logger,
    _: Provider,
)
fun getMovieMetadata(
    context: TmdbContext,
    configuration: TmdbContext.Configuration,
    node: MovieNode,
): Movie {
    val movie = when (val entity = db {
        Movie
            .find(MovieTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> {
            val match = TMDB_ID_REGEX.matchEntire(node.path.name)
            val movieId = match?.let { it.groupValues[1].toInt() }

            val details = if (movieId != null)
                context.getMovieDetails(movieId)
            else null

            db {
                Movie.new {
                    this.path = node.path

                    if (details != null) {
                        this.tmdbId = details.id
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
                        this.tmdbId = null
                        this.title = node.path.name
                        this.description = null
                        this.poster = emptyList()
                        this.backdrop = emptyList()
                    }
                }
            }
        }

        else -> entity
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
    val episode = when (val entity = db {
        Episode
            .find(EpisodeTable.path eq path)
            .firstOrNull()
    }) {
        null -> {
            val match = EPISODE_NUMBER_REGEX.matchEntire(path.name)
            val seasonNumber = match?.let { it.groupValues[1].toInt() }
            val episodeNumber = match?.let { it.groupValues[2].toInt() }

            val details = if (seasonNumber != null && episodeNumber != null) {
                context.getEpisodeDetails(show.tmdbId ?: 0, seasonNumber, episodeNumber)
            } else null

            db {
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

        else -> entity
    }

    return episode
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
    val season = when (val entity = db {
        Season
            .find(SeasonTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> {
            val match = SEASON_NUMBER_REGEX.matchEntire(node.path.name)
            val seasonNumber = match?.let { it.groupValues[1].toInt() }

            val details = if (seasonNumber != null) {
                context.getSeasonDetails(show.tmdbId ?: 0, seasonNumber)
            } else null

            db {
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
        }

        else -> entity
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
    val show = when (val entity = db {
        Show
            .find(ShowTable.path eq node.path)
            .firstOrNull()
    }) {
        null -> {
            val match = TMDB_ID_REGEX.matchEntire(node.path.name)
            val showId = match?.let { it.groupValues[1].toInt() }
            val details = when (showId) {
                null -> null
                else -> context.getShowDetails(showId)
            }

            db {
                Show.new {
                    this.path = node.path

                    if (details != null) {
                        this.tmdbId = details.id
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
                        this.tmdbId = null
                        this.title = node.path.name
                        this.description = null
                        this.poster = emptyList()
                        this.backdrop = emptyList()
                    }
                }
            }
        }

        else -> entity
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

    val groups = when (val id = show.tmdbId) {
        null -> null
        else -> context.getShowEpisodeGroups(id)
    }

    if (groups != null) {
        for (result in groups.results) {
            val details = context.getEpisodeGroupDetails(result.id) ?: continue

            val parent = when (val entity = db {
                ParentGroup
                    .find(ParentGroupTable.tmdbId eq result.id)
                    .firstOrNull()
            }) {
                null -> db {
                    ParentGroup.new {
                        this.show = show
                        this.tmdbId = details.id
                        this.title = details.name
                        this.description = details.description
                    }
                }

                else -> entity
            }

            for (group in details.groups) {
                val child = when (val entity = db {
                    Group
                        .find(GroupTable.tmdbId eq group.id)
                        .firstOrNull()
                }) {
                    null -> db {
                        Group.new {
                            this.parent = parent
                            this.tmdbId = group.id
                            this.title = group.name
                            this.index = group.order
                        }
                    }

                    else -> entity
                }

                db {
                    val episodes = group.episodes
                        .mapNotNull { ref ->
                            val episode = show.seasons
                                .firstOrNull { it.index == ref.seasonNumber }
                                ?.episodes
                                ?.firstOrNull { it.index == ref.episodeNumber }

                            if (episode != null)
                                episode to ref.order
                            else null
                        }
                        .filter { it.first !in child.episodes }

                    EpisodeGroupTable.batchInsert(episodes) { (episode, index) ->
                        this[EpisodeGroupTable.episode] = episode.id
                        this[EpisodeGroupTable.group] = child.id
                        this[EpisodeGroupTable.index] = index
                    }
                }
            }
        }
    }

    return show
}

fun getOtherMetadata(path: Path): Other {
    when (val other = db {
        Other
            .find(OtherTable.path eq path)
            .firstOrNull()
    }) {
        null -> Unit
        else -> return other
    }

    val media = db {
        Media
            .find(MediaTable.path eq path)
            .first()
    }

    return db {
        Other.new {
            this.media = media
            this.path = path
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
