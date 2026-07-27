package dev.scriptor.context

import dev.scriptor.SQL
import dev.scriptor.eq
import dev.scriptor.model.Media
import dev.scriptor.query
import dev.scriptor.select
import dev.scriptor.server.annotation.Context
import dev.scriptor.server.annotation.Inject
import java.sql.Connection
import kotlin.uuid.Uuid

@Context("media")
class MediaContext {

    @Inject("connection")
    lateinit var connection: Connection

    fun getAllMedia(): List<Media> = SQL(connection)
        .select<Media>()
        .query<Media>()

    fun getMediaById(id: Uuid): Media? = SQL(connection)
        .select<Media>()
        .where(Media::id eq id)
        .limit(1)
        .query<Media>()
        .firstOrNull()
}
