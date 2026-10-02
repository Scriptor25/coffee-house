package dev.scriptor.model.media

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
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

@JsonSerializable
class Media(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Media>(MediaTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @JsonProperty
    var path by MediaTable.path

    @JsonProperty
    var size by MediaTable.size

    @JsonProperty
    var title by MediaTable.title

    @JsonProperty
    var createdAt by MediaTable.createdAt

    @JsonProperty
    var modifiedAt by MediaTable.modifiedAt

    @JsonProperty
    var duration by MediaTable.duration

    @JsonProperty
    val video by VideoTrack referrersOn VideoTrackTable.media orderBy VideoTrackTable.index

    @JsonProperty
    val audio by AudioTrack referrersOn AudioTrackTable.media orderBy AudioTrackTable.index

    @JsonProperty
    val subtitles by SubtitleTrack referrersOn SubtitleTrackTable.media orderBy SubtitleTrackTable.index

    @JsonProperty
    val chapters by Chapter referrersOn ChapterTable.media orderBy ChapterTable.index

    override fun toString(): String {
        return "Media(id=$id, path=$path, size=$size, title=$title, createdAt=$createdAt, modifiedAt=$modifiedAt, duration=$duration)"
    }
}
