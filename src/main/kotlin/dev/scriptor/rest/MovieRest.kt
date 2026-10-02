package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.PlaybackItem
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.core.SortOrder
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/movie")
class MovieRest {

    @Get("/[id]", "application/json")
    fun getMovie(@PathParameter id: Uuid): Movie {
        return db { Movie.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    fun getMovieList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Movie> {
        return db {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, context: PlaybackContext)
    fun createMoviePlayback(@PathParameter id: Uuid): String {
        val movie = db { Movie.findById(id) }
            ?: throw NotFoundSignal()

        val items = db {
            movie.items.map {
                PlaybackItem(it.id.value, it.title)
            }
        }

        return context.createPlayback(
            principal.id,
            movie.title,
            items,
        )
    }
}
