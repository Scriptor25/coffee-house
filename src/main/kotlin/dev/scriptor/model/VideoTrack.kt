package dev.scriptor.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object VideoTrackTable : UuidTable("video_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = text("codec")
    val width = integer("width")
    val height = integer("height")
    val bitRate = long("bit_rate")
    val frameRate = double("frame_rate")
    val profile = text("profile").nullable()
    val level = integer("level").nullable()
    val hdr = bool("hdr")
    val language = text("language").nullable()
    val title = text("title").nullable()
    val default = bool("default")

    init {
        uniqueIndex(media, index)
    }
}

class VideoTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<VideoTrack>(VideoTrackTable)

    var media by Media referencedOn VideoTrackTable.media
    var index by VideoTrackTable.index
    var codec by VideoTrackTable.codec
    var width by VideoTrackTable.width
    var height by VideoTrackTable.height
    var bitRate by VideoTrackTable.bitRate
    var frameRate by VideoTrackTable.frameRate
    var profile by VideoTrackTable.profile
    var level by VideoTrackTable.level
    var hdr by VideoTrackTable.hdr
    var language by VideoTrackTable.language
    var title by VideoTrackTable.title
    var default by VideoTrackTable.default

    override fun toString(): String {
        return "VideoTrack(id=$id, media=$media, index=$index, codec=$codec, width=$width, height=$height, bitRate=$bitRate, frameRate=$frameRate, profile=$profile, level=$level, hdr=$hdr, language=$language, title=$title, default=$default)"
    }
}
