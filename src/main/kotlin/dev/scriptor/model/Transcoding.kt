package dev.scriptor.model

import dev.scriptor.model.media.Media
import dev.scriptor.model.media.MediaTable
import dev.scriptor.model.media.instant
import dev.scriptor.model.media.path
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

enum class TranscodingState {
    CREATED,
    PROCESSING,
    FINISHED,
    FAILED,
}

object TranscodingTable : UuidTable("transcoding") {
    val media = reference("media_id", MediaTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val path = path("path")
    val state = enumeration("state", TranscodingState::class)
    val access = instant("access")

    init {
        uniqueIndex(media)
    }
}

class Transcoding(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Transcoding>(TranscodingTable)

    var media by Media optionalReferencedOn TranscodingTable.media
    var path by TranscodingTable.path
    var state by TranscodingTable.state
    var access by TranscodingTable.access
}
