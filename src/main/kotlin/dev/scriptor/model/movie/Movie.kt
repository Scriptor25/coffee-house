package dev.scriptor.model.movie

import dev.scriptor.model.media.Media
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object MovieTable : UuidTable("movie") {
    val tmdbId = integer("tmdb_id").nullable()
    val title = text("title")

    init {
        uniqueIndex(tmdbId)
    }
}

class Movie(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Movie>(MovieTable)

    var tmdbId by MovieTable.tmdbId
    var title by MovieTable.title

    val items by Media via MovieMediaTable

    override fun toString(): String {
        return "Movie(id=$id, tmdbId=$tmdbId, title=$title)"
    }
}
