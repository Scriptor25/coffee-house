package dev.scriptor.model.movie

import dev.scriptor.model.media.MediaTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object MovieMediaTable : Table("movie_media") {
    val movie = reference("movie_id", MovieTable, onDelete = ReferenceOption.CASCADE)
    val media = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val extra = bool("extra")

    override val primaryKey = PrimaryKey(movie, media)
}
