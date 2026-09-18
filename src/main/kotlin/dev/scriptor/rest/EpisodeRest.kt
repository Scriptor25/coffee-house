package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.show.Episode
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/resource/episode")
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
        @Header cookie: CookieHeader = CookieHeader(),
    ): Episode {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
        context: PlaybackContext,
    )
    fun createEpisodePlayback(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val session = auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        val userId = session.user?.id?.value

        val episode = transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val item = transaction(database) { episode.media.id.value }

        return context.createPlayback(
            userId,
            episode.title,
            listOf(item),
        )
    }
}
