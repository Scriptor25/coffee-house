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

object AudioTrackTable : UuidTable("audio_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = reference("codec", CodecTable, ReferenceOption.CASCADE)
    val bitRate = long("bit_rate")
    val sampleRate = long("sample_rate")
    val channels = integer("channels")
    val language = text("language").nullable()
    val title = text("title").nullable()
    val default = bool("default")
    val forced = bool("forced")

    init {
        uniqueIndex(media, index)
    }
}

@JsonSerializable
class AudioTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<AudioTrack>(AudioTrackTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var media by Media referencedOn AudioTrackTable.media

    @JsonProperty
    var index by AudioTrackTable.index

    @JsonProperty
    var codec by Codec referencedOn AudioTrackTable.codec

    @JsonProperty
    var bitRate by AudioTrackTable.bitRate

    @JsonProperty
    var sampleRate by AudioTrackTable.sampleRate

    @JsonProperty
    var channels by AudioTrackTable.channels

    @JsonProperty
    var language by AudioTrackTable.language

    @JsonProperty
    var title by AudioTrackTable.title

    @JsonProperty
    var default by AudioTrackTable.default

    @JsonProperty
    var forced by AudioTrackTable.forced

    override fun toString(): String {
        return "AudioTrack(id=$id, media=${media.id}, index=$index, codec=${codec.id}, bitRate=$bitRate, sampleRate=$sampleRate, channels=$channels, language=$language, title=$title, default=$default)"
    }
}
