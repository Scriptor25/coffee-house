package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.db
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.PlaybackItem
import dev.scriptor.model.other.Other
import dev.scriptor.model.other.OtherTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.core.SortOrder
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/other")
class OtherRest {

    @Get("/[id]", "application/json")
    fun getOther(@PathParameter id: Uuid): Other {
        return db { Other.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    fun getOtherList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Other> {
        return db {
            Other
                .all()
                .orderBy(OtherTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, context: PlaybackContext)
    fun createOtherPlayback(@PathParameter id: Uuid): String {
        val other = db { Other.findById(id) }
            ?: throw NotFoundSignal()

        val item = db { PlaybackItem(other.media.id.value, other.title) }

        return context.createPlayback(
            principal.id,
            other.title,
            listOf(item),
        )
    }
}
