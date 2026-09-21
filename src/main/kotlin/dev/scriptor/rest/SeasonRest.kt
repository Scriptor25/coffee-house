package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Episode
import dev.scriptor.model.show.Season
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/season")
class SeasonRest {

    @Get("/[id]", "application/json")
    fun getSeason(@PathParameter id: Uuid): Season {
        return db { Season.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/episodes", "application/json", "application/json")
    fun getSeasonEpisodes(
        @PathParameter id: Uuid,
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Episode> {
        val season = db { Season.findById(id) }
            ?: throw NotFoundSignal()

        return db {
            season.episodes
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, context: PlaybackContext)
    fun createSeasonPlayback(@PathParameter id: Uuid): String {
        val season = db { Season.findById(id) }
            ?: throw NotFoundSignal()

        val items = db { season.episodes.map { it.media.id.value } }

        return context.createPlayback(
            principal.id,
            season.title,
            items,
        )
    }
}
