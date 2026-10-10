package dev.scriptor.model.show

import dev.scriptor.*
import dev.scriptor.model.media.path
import dev.scriptor.model.movie.ImageData
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SeasonTable : UuidTable("season") {
    val path = path("path").uniqueIndex()
    val show = reference("show_id", ShowTable, onDelete = ReferenceOption.CASCADE)
    val index = integer("index")
    val title = text("title")
    val description = text("description").nullable()
    val poster = json<List<ImageData>>(
        "poster",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    )
}

@JsonSerializable
class Season(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Season>(SeasonTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @all:JsonProperty
    var path by SeasonTable.path

    var show by Show referencedOn SeasonTable.show

    @all:JsonProperty
    var index by SeasonTable.index

    @all:JsonProperty
    var title by SeasonTable.title

    @all:JsonProperty
    var description by SeasonTable.description

    @all:JsonProperty
    var poster by SeasonTable.poster

    val episodes by Episode referrersOn EpisodeTable.season orderBy EpisodeTable.index

    @all:JsonProperty("episodes")
    val jsonEpisodes
        get() = episodes.map { it.id.value }

    override fun toString(): String {
        return "Season(id=$id, show=${show.id}, index=$index)"
    }
}
