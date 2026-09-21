package dev.scriptor.rest

import dev.scriptor.context.PlaybackContext
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.other.Other
import dev.scriptor.model.other.OtherTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/other")
class OtherRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getOther(@PathParameter id: Uuid): Other {
        return transaction(database) { Other.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(database: Database)
    fun getOtherList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Other> {
        return transaction(database) {
            Other
                .all()
                .orderBy(OtherTable.title to SortOrder.ASC)
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }

    @Post("/[id]/playback", "*/*", "text/plain")
    context(principal: Principal, database: Database, context: PlaybackContext)
    fun createOtherPlayback(@PathParameter id: Uuid): String {
        val other = transaction(database) { Other.findById(id) }
            ?: throw NotFoundSignal()

        val item = transaction(database) { other.media.id.value }

        return context.createPlayback(
            principal.id,
            other.title,
            listOf(item),
        )
    }
}
