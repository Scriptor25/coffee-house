package dev.scriptor.model.show

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

data object GroupTable : UuidTable("group") {
    val parent = reference("parent_id", ParentGroupTable, onDelete = ReferenceOption.CASCADE)
    val tmdbId = text("tmdb_id").nullable()
    val title = text("title")
    val index = integer("index")
}

class Group(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Group>(GroupTable)

    var parent by ParentGroup referencedOn GroupTable.parent
    var tmdbId by GroupTable.tmdbId
    var title by GroupTable.title
    var index by GroupTable.index

    val episodes by Episode via EpisodeGroupTable orderBy EpisodeGroupTable.index

    override fun toString(): String {
        return "Group(id=$id, title=$title)"
    }
}
