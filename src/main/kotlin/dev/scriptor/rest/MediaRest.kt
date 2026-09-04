package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.emptyJsonObject
import dev.scriptor.get
import dev.scriptor.model.Authorization
import dev.scriptor.model.media.Media
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.uuid.Uuid

@Suppress("unused")
@Controller("/media")
class MediaRest {

    @Get("/[id]", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMedia(
        @PathParameter id: Uuid,
        @Header authorization: Authorization? = null,
    ): Media {
        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        return transaction(database) { Media.findById(id) }
            ?: throw NotFoundSignal()
    }

    @Post("/list", "application/json", "application/json")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun getMediaList(
        @Header authorization: Authorization? = null,
        @Body body: JsonNode? = null,
    ): List<Media> {
        val body = body ?: emptyJsonObject()

        auth.auth(authorization)
            ?: throw UnauthorizedSignal()

        val offset = body["offset"].get<Number?>()?.toLong() ?: 0L
        val limit = body["limit"].get<Number?>()?.toInt() ?: Int.MAX_VALUE

        return transaction(database) {
            Media
                .all()
                .offset(offset)
                .limit(limit)
                .toList()
        }
    }
}
