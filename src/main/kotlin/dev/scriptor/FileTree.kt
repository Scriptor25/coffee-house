package dev.scriptor

import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.name
import kotlin.io.path.visitFileTree

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

                else -> Unit
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
