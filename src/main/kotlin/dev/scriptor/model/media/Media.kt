package dev.scriptor.model.media

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import java.nio.file.Path
import java.sql.Timestamp
import kotlin.io.path.Path
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

fun Table.instant(name: String): Column<Instant> = registerColumn(
    name,
    object : ColumnType<Instant>() {
        override fun sqlType(): String {
            return "TIMESTAMP"
        }

        override fun valueFromDB(value: Any): Instant? = when (value) {
            is Instant -> value
            is Timestamp -> value.toInstant().toKotlinInstant()
            is String -> Instant.parse(value)
            else -> error("unexpected value of type ${value::class}")
        }
    },
)

fun Table.path(name: String): Column<Path> = registerColumn(
    name,
    object : ColumnType<Path>() {
        override fun sqlType(): String {
            return "TEXT"
        }

        override fun valueFromDB(value: Any): Path? = when (value) {
            is Path -> value
            is String -> Path(value)
            else -> error("unexpected value of type ${value::class}")
        }
    },
)

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
        return "Media(id=$id, path=$path, size=$size, title=$title, createdAt=$createdAt, modifiedAt=$modifiedAt, duration=$duration)"
    }
}
