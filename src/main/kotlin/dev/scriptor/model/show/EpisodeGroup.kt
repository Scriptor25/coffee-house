package dev.scriptor.model.show

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

data object EpisodeGroupTable : Table("episode_group") {
    val episode = reference("episode_id", EpisodeTable, onDelete = ReferenceOption.CASCADE)
    val group = reference("group_id", GroupTable, onDelete = ReferenceOption.CASCADE)
    val index = integer("index")

    init {
        uniqueIndex(episode, group)
    }
}
