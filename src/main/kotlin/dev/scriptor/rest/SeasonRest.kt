package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Episode
import dev.scriptor.model.show.Season
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/resource/season")
class SeasonRest {

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getSeason(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): Season {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/episodes", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getSeasonEpisodes(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Episode> {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

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
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
        context: PlaybackContext,
    )
    fun createSeasonPlayback(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): String {
        val session = auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        val userId = session.user?.id?.value

        val season = transaction(database) { Season.findById(id) }
            ?: throw NotFoundSignal()

        val items = transaction(database) { season.episodes.map { it.media.id.value } }

        return context.createPlayback(
            userId,
            season.title,
            items,
        )
    }
}
