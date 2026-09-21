package dev.scriptor.model.other

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.MediaTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object OtherTable : UuidTable("other") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val title = text("title")
}

@JsonSerializable
class Other(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Other>(OtherTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var media by Media referencedOn OtherTable.media

    @JsonProperty
    var title by OtherTable.title

    @all:JsonProperty("item")
    val jsonItem
        get() = media.id.value

    override fun toString(): String {
        return "Other(id=$id, media=${media.id}, title=$title)"
    }
}
