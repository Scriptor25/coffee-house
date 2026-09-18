package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Season
import dev.scriptor.model.show.Show
import dev.scriptor.model.show.ShowTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/resource/show")
class ShowRest {

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getShow(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): Show {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Show.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/seasons", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getShowSeasons(
        @PathParameter id: Uuid,
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Season> {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        val show = transaction(database) { Show.findById(id) }
            ?: throw NotFoundSignal()

        return transaction(database) {
            show.seasons
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/list", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getShowList(
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Show> {
        auth.auth(authorization, cookie)
            ?: throw UnauthorizedSignal()

        return transaction(database) {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }
}
