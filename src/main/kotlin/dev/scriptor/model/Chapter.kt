package dev.scriptor.model

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

class Chapter(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Chapter>(ChapterTable)

    var media by Media referencedOn ChapterTable.media
    var index by ChapterTable.index
    var start by ChapterTable.start
    var end by ChapterTable.end
    var language by ChapterTable.language
    var title by ChapterTable.title

    override fun toString(): String {
        return "Chapter(id=$id, index=$index, start=$start, end=$end, language=$language, title=$title)"
    }
}
