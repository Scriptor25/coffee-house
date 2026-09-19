package dev.scriptor.rest

import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Season
import dev.scriptor.model.show.Show
import dev.scriptor.model.show.ShowTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/show")
class ShowRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getShow(@PathParameter id: Uuid): Show {
        return transaction(database) { Show.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/seasons", "application/json", "application/json")
    context(database: Database)
    fun getShowSeasons(
        @PathParameter id: Uuid,
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Season> {
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
    context(database: Database)
    fun getShowList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Show> {
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
