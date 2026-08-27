package dev.scriptor.model.media

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SubtitleTrackTable : UuidTable("subtitle_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = text("codec")
    val language = text("language").nullable()
    val title = text("title").nullable()
    val default = bool("default")
    val forced = bool("forced")

    init {
        uniqueIndex(media, index)
    }
}

class SubtitleTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<SubtitleTrack>(SubtitleTrackTable)

    var media by Media.Companion referencedOn SubtitleTrackTable.media
    var index by SubtitleTrackTable.index
    var codec by SubtitleTrackTable.codec
    var language by SubtitleTrackTable.language
    var title by SubtitleTrackTable.title
    var default by SubtitleTrackTable.default
    var forced by SubtitleTrackTable.forced

    override fun toString(): String {
        return "SubtitleTrack(id=$id, media=$media, index=$index, codec=$codec, language=$language, title=$title, default=$default, forced=$forced)"
    }
}
