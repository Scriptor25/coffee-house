package dev.scriptor.model.media

import dev.scriptor.instant
import dev.scriptor.path
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object MediaTable : UuidTable("media") {
    val path = path("path").uniqueIndex()
    val size = long("size")
    val title = text("title")
    val createdAt = instant("created_at")
    val modifiedAt = instant("modified_at")
    val duration = double("duration")
}

class Media(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Media>(MediaTable)

    var path by MediaTable.path
    var size by MediaTable.size
    var title by MediaTable.title
    var createdAt by MediaTable.createdAt
    var modifiedAt by MediaTable.modifiedAt
    var duration by MediaTable.duration

    val video by VideoTrack referrersOn VideoTrackTable.media
    val audio by AudioTrack referrersOn AudioTrackTable.media
    val subtitles by SubtitleTrack referrersOn SubtitleTrackTable.media
    val chapters by Chapter referrersOn ChapterTable.media

    override fun toString(): String {
        return "Media(id=$id, path=$path, size=$size, createdAt=$createdAt, modifiedAt=$modifiedAt, duration=$duration)"
    }
}
