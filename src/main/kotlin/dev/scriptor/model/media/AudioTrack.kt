package dev.scriptor.model.media

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object AudioTrackTable : UuidTable("audio_track") {
    val media = reference("media_id", MediaTable, ReferenceOption.CASCADE)
    val index = integer("index")
    val codec = text("codec")
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

class AudioTrack(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<AudioTrack>(AudioTrackTable)

    var media by Media.Companion referencedOn AudioTrackTable.media
    var index by AudioTrackTable.index
    var codec by AudioTrackTable.codec
    var bitRate by AudioTrackTable.bitRate
    var sampleRate by AudioTrackTable.sampleRate
    var channels by AudioTrackTable.channels
    var language by AudioTrackTable.language
    var title by AudioTrackTable.title
    var default by AudioTrackTable.default
    var forced by AudioTrackTable.forced

    override fun toString(): String {
        return "AudioTrack(id=$id, media=$media, index=$index, codec=$codec, bitRate=$bitRate, sampleRate=$sampleRate, channels=$channels, language=$language, title=$title, default=$default)"
    }
}
