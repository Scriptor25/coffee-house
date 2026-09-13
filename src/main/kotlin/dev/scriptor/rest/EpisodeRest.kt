package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.show.Episode
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.Controller
import dev.scriptor.server.jvm.annotation.Get
import dev.scriptor.server.jvm.annotation.Header
import dev.scriptor.server.jvm.annotation.PathParameter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/episode")
class EpisodeRest {

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getEpisode(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
    ): Episode {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()
    }
}
