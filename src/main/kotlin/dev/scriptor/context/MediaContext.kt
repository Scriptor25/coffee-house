package dev.scriptor.context

import dev.scriptor.SQL
import dev.scriptor.eq
import dev.scriptor.model.Media
import dev.scriptor.query
import dev.scriptor.selectFrom
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import java.sql.Connection
import kotlin.uuid.Uuid

@Context
class MediaContext {

    context(
        _: Provider,
        connection: Connection,
    )
    fun getAllMedia(): List<Media> = SQL(connection)
        .selectFrom<Media>()
        .query<Media>()

    context(
        _: Provider,
        connection: Connection,
    )
    fun getMediaById(id: Uuid): Media? = SQL(connection)
        .selectFrom<Media>()
        .where(Media::id eq id)
        .limit(1)
        .query<Media>()
        .firstOrNull()
}
