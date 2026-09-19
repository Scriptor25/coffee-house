package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Episode
import dev.scriptor.model.show.Season
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/season")
class SeasonRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getSeason(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): Season {
        return transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/episodes", "application/json", "application/json")
    context(database: Database)
    fun getSeasonEpisodes(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Episode> {
        val season = transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()

        return transaction(database) {
            season.episodes
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, database: Database, context: PlaybackContext)
    fun createSeasonPlayback(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val season = transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()

        val items = transaction(database) { season.episodes.map { it.media.id.value } }

        return context.createPlayback(
            principal.id,
            season.title,
            items,
        )
    }
}
