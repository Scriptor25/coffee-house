package dev.scriptor.model.show

import dev.scriptor.model.media.Media
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object EpisodeTable : UuidTable("episode") {
    val season = reference("season_id", SeasonTable, ReferenceOption.CASCADE)
    val index = integer("index")

    init {
        uniqueIndex(season, index)
    }
}

class Episode(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Episode>(EpisodeTable)

    var season by Season referencedOn EpisodeTable.season
    var index by EpisodeTable.index

    val items by Media via EpisodeMediaTable

    override fun toString(): String {
        return "Episode(id=$id, season=${season.id}, index=$index)"
    }
}
