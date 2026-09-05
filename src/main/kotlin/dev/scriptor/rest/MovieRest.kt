package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.Authorization
import dev.scriptor.model.OffsetLimit
import dev.scriptor.model.movie.Movie
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
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
        @Header authorization: Authorization? = null,
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
        @Header authorization: Authorization? = null,
        @Body body: OffsetLimit = OffsetLimit(),
    ): List<Movie> {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) {
            Movie
                .all()
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }
}
