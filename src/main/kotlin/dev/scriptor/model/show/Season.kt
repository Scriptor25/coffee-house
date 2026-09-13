package dev.scriptor.model.show

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SeasonTable : UuidTable("season") {
    val show = reference("show_id", ShowTable, ReferenceOption.CASCADE)
    val index = integer("index")

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
    val episodes by Episode referrersOn EpisodeTable.season

    override fun toString(): String {
        return "Season(id=$id, show=${show.id}, index=$index)"
    }
}
