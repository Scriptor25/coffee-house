package dev.scriptor.model.show

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

data object ParentGroupTable : UuidTable("parent_group") {
    val show = reference("show_id", ShowTable, onDelete = ReferenceOption.CASCADE)
    val tmdbId = text("tmdb_id")
    val title = text("title")
    val description = text("description")
}

class ParentGroup(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ParentGroup>(ParentGroupTable)

    var show by ParentGroupTable.show
    var tmdbId by ParentGroupTable.tmdbId
    var title by ParentGroupTable.title
    var description by ParentGroupTable.description

    val groups by Group referrersOn GroupTable.parent
}
