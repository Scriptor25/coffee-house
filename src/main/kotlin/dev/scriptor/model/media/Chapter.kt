package dev.scriptor.model.media

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object ChapterTable : UuidTable("chapter") {
    val media = reference("media_id", MediaTable.id, ReferenceOption.CASCADE)
    val index = integer("index")
    val start = double("start")
    val end = double("end")
    val language = text("language").nullable()
    val title = text("title").nullable()

    init {
        uniqueIndex(media, index)
    }
}

@JsonSerializable
class Chapter(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Chapter>(ChapterTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    var media by Media referencedOn ChapterTable.media

    @JsonProperty
    var index by ChapterTable.index

    @JsonProperty
    var start by ChapterTable.start

    @JsonProperty
    var end by ChapterTable.end

    @JsonProperty
    var language by ChapterTable.language

    @JsonProperty
    var title by ChapterTable.title

    override fun toString(): String {
        return "Chapter(id=$id, media=${media.id} index=$index, start=$start, end=$end, language=$language, title=$title)"
    }
}
