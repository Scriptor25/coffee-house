package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.Authorization
import dev.scriptor.model.media.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/media")
class MediaRest {

    @Get("/", result = "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun getMediaList(
        @QueryParameter offset: Long?,
        @QueryParameter limit: Int?,
        @Header authorization: Authorization,
    ): List<Media> {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) {
            Media
                .all()
                .offset(offset ?: 0L)
                .limit(limit ?: Int.MAX_VALUE)
                .toList()
        }
    }

    @Get("/[id]", result = "application/json")
    context(
        database: Database,
        auth: AuthContext,
    )
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Authorization?,
    ): Media {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()
    }
}
