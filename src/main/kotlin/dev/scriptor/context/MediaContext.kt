package dev.scriptor.context

import dev.scriptor.EntityConnection
import dev.scriptor.get
import dev.scriptor.getAll
import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import kotlin.uuid.Uuid

@Context
class MediaContext {

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getAllMedia(): List<Media> =
        connection.getAll<Media>()

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getMediaById(id: Uuid): Media? =
        connection.get<Media>(id)
}
