package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/movie")
class MovieRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getMovie(@PathParameter id: Uuid): Movie {
        return transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(database: Database)
    fun getMovieList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Movie> {
        return transaction(database) {
            Movie
                .all()
                .orderBy(MovieTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, database: Database, context: PlaybackContext)
    fun createMoviePlayback(@PathParameter id: Uuid): String {
        val movie = transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()

        val items = transaction(database) { movie.items.map { it.id.value } }

        return context.createPlayback(
            principal.id,
            movie.title,
            items,
        )
    }
}
