package dev.scriptor.model.show

import dev.scriptor.*
import dev.scriptor.model.media.path
import dev.scriptor.model.movie.ImageData
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object ShowTable : UuidTable("show") {
    val path = path("path").uniqueIndex()
    val tmdbId = integer("tmdb_id").nullable()
    val title = text("title")
    val description = text("description").nullable()
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
}

@JsonSerializable
class Show(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Show>(ShowTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @JsonProperty
    var path by ShowTable.path

    @JsonProperty
    var tmdbId by ShowTable.tmdbId

    @JsonProperty
    var title by ShowTable.title

    @JsonProperty
    var description by ShowTable.description

    @JsonProperty
    var poster by ShowTable.poster

    @JsonProperty
    var backdrop by ShowTable.backdrop

    val seasons by Season referrersOn SeasonTable.show orderBy SeasonTable.index

    @all:JsonProperty("seasons")
    val jsonSeasons
        get() = seasons.map { it.id.value }

    val groups by ParentGroup referrersOn ParentGroupTable.show orderBy ParentGroupTable.title

    override fun toString(): String {
        return "Show(id=$id, path=$path, tmdbId=$tmdbId, title=$title, description=$description, poster=$poster, backdrop=$backdrop)"
    }
}
