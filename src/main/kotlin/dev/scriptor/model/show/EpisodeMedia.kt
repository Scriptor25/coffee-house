package dev.scriptor.model.show

import dev.scriptor.model.media.MediaTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object EpisodeMediaTable : Table("episode_media") {
    val episode = reference("episode_id", EpisodeTable.id, ReferenceOption.CASCADE)
    val media = reference("media_id", MediaTable.id, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(episode, media)
}
