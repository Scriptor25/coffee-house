package dev.scriptor.model.show

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

object EpisodeTable : UuidTable("episode") {
    val season = reference("season_id", SeasonTable, ReferenceOption.CASCADE)
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")

    init {
        uniqueIndex(season, index)
    }
}

@JsonSerializable
class Episode(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Episode>(EpisodeTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var season by Season referencedOn EpisodeTable.season

    @JsonProperty
    var media by Media referencedOn EpisodeTable.media

    @JsonProperty
    var index by EpisodeTable.index

    override fun toString(): String {
        return "Episode(id=$id, season=${season.id}, media=${media.id}, index=$index)"
    }
}
