package dev.scriptor.model.show

import dev.scriptor.*
import dev.scriptor.model.movie.ImageData
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SeasonTable : UuidTable("season") {
    val show = reference("show_id", ShowTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val title = text("title")
    val description = text("description").nullable()
    val poster = json<List<ImageData>>(
        "poster",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    )

    init {
        uniqueIndex(show, index)
    }
}

@JsonSerializable
class Season(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Season>(SeasonTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var show by Show referencedOn SeasonTable.show

    @JsonProperty
    var index by SeasonTable.index

    @JsonProperty
    var title by SeasonTable.title

    @JsonProperty
    var description by SeasonTable.description

    @JsonProperty
    var poster by SeasonTable.poster

    val episodes by Episode referrersOn EpisodeTable.season

    @all:JsonProperty("episodes")
    val jsonEpisodes
        get() = episodes.orderBy(EpisodeTable.index to SortOrder.ASC).map { it.id.value }

    override fun toString(): String {
        return "Season(id=$id, show=${show.id}, index=$index)"
    }
}
