package dev.scriptor.model.media

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import dev.scriptor.model.ffmpeg.Codec
import dev.scriptor.model.ffmpeg.CodecTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object VideoTrackTable : UuidTable("video_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = reference("codec", CodecTable, ReferenceOption.CASCADE)
    val width = integer("width")
    val height = integer("height")
    val bitRate = long("bit_rate")
    val frameRate = double("frame_rate")
    val profile = text("profile").nullable().default(null)
    val level = integer("level").nullable().default(null)
    val language = text("language").nullable().default(null)
    val title = text("title").nullable().default(null)
    val default = bool("default")

    init {
        uniqueIndex(media, index)
    }
}

@JsonSerializable
class VideoTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<VideoTrack>(VideoTrackTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var media by Media referencedOn VideoTrackTable.media

    @JsonProperty
    var index by VideoTrackTable.index

    @JsonProperty
    var codec by Codec referencedOn VideoTrackTable.codec

    @JsonProperty
    var width by VideoTrackTable.width

    @JsonProperty
    var height by VideoTrackTable.height

    @JsonProperty
    var bitRate by VideoTrackTable.bitRate

    @JsonProperty
    var frameRate by VideoTrackTable.frameRate

    @JsonProperty
    var profile by VideoTrackTable.profile

    @JsonProperty
    var level by VideoTrackTable.level

    @JsonProperty
    var language by VideoTrackTable.language

    @JsonProperty
    var title by VideoTrackTable.title

    @JsonProperty
    var default by VideoTrackTable.default

    override fun toString(): String {
        return "VideoTrack(id=$id, media=${media.id}, index=$index, codec=${codec.id}, width=$width, height=$height, bitRate=$bitRate, frameRate=$frameRate, profile=$profile, level=$level, language=$language, title=$title, default=$default)"
    }
}
