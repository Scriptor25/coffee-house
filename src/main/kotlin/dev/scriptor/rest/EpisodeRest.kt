package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.model.PlaybackItem
import dev.scriptor.model.show.Episode
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/episode")
class EpisodeRest {

    @Get("/[id]", "application/json")
    fun getEpisode(@PathParameter id: Uuid): Episode {
        return db { Episode.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, context: PlaybackContext)
    fun createEpisodePlayback(@PathParameter id: Uuid): String {
        val episode = db { Episode.findById(id) }
            ?: throw NotFoundSignal()

        val item = db { PlaybackItem(episode.media.id.value, episode.title) }

        return context.createPlayback(
            principal.id,
            episode.title,
            listOf(item),
        )
    }
}
