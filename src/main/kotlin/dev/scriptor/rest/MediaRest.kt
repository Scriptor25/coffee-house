package dev.scriptor.rest

import dev.scriptor.db
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.model.media.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.jvm.annotation.*
import kotlin.uuid.Uuid

@RequireAuth
@Controller("/resource/media")
class MediaRest {

    @Get("/[id]", "application/json")
    fun getMedia(@PathParameter id: Uuid): Media {
        return db { Media.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    fun getMediaList(@Body body: OffsetLimitBody = OffsetLimitBody()): List<Media> {
        return db {
            Media
                .all()
                .offset(body.offset)
                .limit(body.limit)
                .toList()
        }
    }
}
