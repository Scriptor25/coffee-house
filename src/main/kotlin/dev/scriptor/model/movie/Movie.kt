package dev.scriptor.model.movie

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import dev.scriptor.model.media.Media
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object MovieTable : UuidTable("movie") {
    val tmdbId = integer("tmdb_id").nullable()
    val title = text("title")
    val description = text("description").nullable()
    val poster = text("poster").nullable()
    val backdrop = text("backdrop").nullable()

    init {
        uniqueIndex(tmdbId)
    }
}

@JsonSerializable
class Movie(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Movie>(MovieTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @JsonProperty
    var tmdbId by MovieTable.tmdbId

    @JsonProperty
    var title by MovieTable.title

    @JsonProperty
    var description by MovieTable.description

    @JsonProperty
    var poster by MovieTable.poster

    @JsonProperty
    var backdrop by MovieTable.backdrop

    @JsonProperty
    val items by Media via MovieMediaTable

    override fun toString(): String {
        return "Movie(id=$id, tmdbId=$tmdbId, title=$title, description=$description, poster=$poster, backdrop=$backdrop)"
    }
}
