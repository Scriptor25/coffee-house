package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.movie.Movie
import dev.scriptor.model.movie.MovieTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/movie")
class MovieRest {

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMovie(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
    ): Movie {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMovieList(
        @Header authorization: AuthorizationHeader? = null,
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Movie> {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

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
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
        context: PlaybackContext,
    )
    fun createMoviePlayback(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
    ): String {
        val session = auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        val userId = session.user?.id?.value

        val movie = transaction(database) { Movie.findById(id) }
            ?: throw NotFoundSignal()

        val items = transaction(database) { movie.items.map { it.id.value } }

        return context.createPlayback(
            userId,
            movie.title,
            items,
        )
    }
}
