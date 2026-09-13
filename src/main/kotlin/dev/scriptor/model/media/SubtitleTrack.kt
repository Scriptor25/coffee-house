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

object SubtitleTrackTable : UuidTable("subtitle_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = reference("codec", CodecTable, ReferenceOption.CASCADE)
    val language = text("language").nullable().default(null)
    val title = text("title").nullable().default(null)
    val default = bool("default")
    val forced = bool("forced")

    init {
        uniqueIndex(media, index)
    }
}

@JsonSerializable
class SubtitleTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<SubtitleTrack>(SubtitleTrackTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var media by Media referencedOn SubtitleTrackTable.media

    @JsonProperty
    var index by SubtitleTrackTable.index

    @JsonProperty
    var codec by Codec referencedOn SubtitleTrackTable.codec

    @JsonProperty
    var language by SubtitleTrackTable.language

    @JsonProperty
    var title by SubtitleTrackTable.title

    @JsonProperty
    var default by SubtitleTrackTable.default

    @JsonProperty
    var forced by SubtitleTrackTable.forced

    override fun toString(): String {
        return "SubtitleTrack(id=$id, media=${media.id}, index=$index, codec=${codec.id}, language=$language, title=$title, default=$default, forced=$forced)"
    }
}
