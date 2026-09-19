package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.show.Episode
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/episode")
class EpisodeRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getEpisode(@PathParameter id: Uuid): Episode {
        return transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, database: Database, context: PlaybackContext)
    fun createEpisodePlayback(@PathParameter id: Uuid): String {
        val episode = transaction(database) { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val item = transaction(database) { episode.media.id.value }

        return context.createPlayback(
            principal.id,
            episode.title,
            listOf(item),
        )
    }
}
