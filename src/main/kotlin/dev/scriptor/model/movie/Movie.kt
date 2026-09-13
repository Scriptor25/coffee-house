package dev.scriptor.model.movie

import dev.scriptor.*
import dev.scriptor.model.media.Media
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

@JsonSerializable
data class ImageData(
    @all:JsonProperty
    val url: String,
    @all:JsonProperty
    val width: Int,
)

object MovieTable : UuidTable("movie") {
    val tmdbId = integer("tmdb_id").nullable().default(null)
    val title = text("title")
    val description = text("description").nullable().default(null)
    val poster = json<List<ImageData>>(
        "poster",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    )
    val backdrop = json<List<ImageData>>(
        "backdrop",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    )

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

    val items by Media via MovieMediaTable

    @all:JsonProperty("items")
    val jsonItems
        get() = items.map { it.id.value }

    override fun toString(): String {
        return "Movie(id=$id, tmdbId=$tmdbId, title=$title, description=$description, poster=$poster, backdrop=$backdrop)"
    }
}
