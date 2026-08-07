package dev.scriptor.model

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.ImmutableEntityClass
import org.jetbrains.exposed.v1.dao.UuidEntity
import kotlin.uuid.Uuid

object SubtitleTrackTable : UuidTable("subtitle_track") {
    val media = reference("media_id", MediaTable)
    val index = integer("index")
    val codec = text("codec")
    val language = text("language").nullable()
    val title = text("title").nullable()
    val default = bool("default")
    val forced = bool("forced")

    init {
        uniqueIndex("subtitle_track_media_index", media, index)
    }
}

class SubtitleTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : ImmutableEntityClass<Uuid, SubtitleTrack>(SubtitleTrackTable)

    val media by SubtitleTrackTable.media
    val index by SubtitleTrackTable.index
    val codec by SubtitleTrackTable.codec
    val language by SubtitleTrackTable.language
    val title by SubtitleTrackTable.title
    val default by SubtitleTrackTable.default
    val forced by SubtitleTrackTable.forced

    override fun toString(): String {
        return "SubtitleTrack(id=$id, media=$media, index=$index, codec=$codec, language=$language, title=$title, default=$default, forced=$forced)"
    }
}
