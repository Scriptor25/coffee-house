package dev.scriptor.model.movie

import dev.scriptor.model.media.MediaTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object MovieMediaTable : Table("movie_media") {
    val movie = reference("movie_id", MovieTable.id, ReferenceOption.CASCADE)
    val media = reference("media_id", MediaTable.id, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(movie, media)
}
