package dev.scriptor.rest

import dev.scriptor.db
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.show.Season
import dev.scriptor.model.show.Show
import dev.scriptor.model.show.ShowTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.core.SortOrder
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/show")
class ShowRest {

    @Get("/[id]", "application/json")
    fun getShow(@PathParameter id: Uuid): Show {
        return db { Show.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/[id]/seasons", "application/json", "application/json")
    fun getShowSeasons(
        @PathParameter id: Uuid,
        @Body body: OffsetLimitBody = OffsetLimitBody(),
    ): List<Season> {
        val show = db { Show.findById(id) }
            ?: throw NotFoundSignal()

        return db {
            show.seasons
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/list", "application/json", "application/json")
    fun getShowList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Show> {
        return db {
            Show
                .all()
                .orderBy(ShowTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }
}
