package dev.scriptor.model.show

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object ShowTable : UuidTable("show") {
    val tmdbId = integer("tmdb_id").nullable()
    val title = text("title")
}

@JsonSerializable
class Show(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Show>(ShowTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @JsonProperty
    var tmdbId by ShowTable.tmdbId

    @JsonProperty
    var title by ShowTable.title

    @JsonProperty
    val seasons by Season referrersOn SeasonTable.show

    override fun toString(): String {
        return "Show(id=$id, title=$title)"
    }
}
