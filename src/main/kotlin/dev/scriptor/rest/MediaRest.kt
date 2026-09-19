package dev.scriptor.rest

import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.media.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/media")
class MediaRest {

    @Get("/[id]", "application/json")
    context(database: Database)
    fun getMedia(@PathParameter id: Uuid): Media {
        return transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(database: Database)
    fun getMediaList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Media> {
        return transaction(database) {
            Media
                .all()
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }
}
