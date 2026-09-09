package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object FilterTable : IdTable<FilterId>("filter") {
    override val id = filterId("id").entityId()

    val transform = text("transform")
    val timelineSupport = bool("timeline_support")
    val sliceThreading = bool("slice_threading")
}

class Filter(id: EntityID<FilterId>) : Entity<FilterId>(id) {
    companion object : EntityClass<FilterId, Filter>(FilterTable)

    var transform by FilterTable.transform
    var timelineSupport by FilterTable.timelineSupport
    var sliceThreading by FilterTable.sliceThreading

    override fun toString(): String {
        return "Filter(id=$id, transform=$transform, timelineSupport=$timelineSupport, sliceThreading=$sliceThreading)"
    }
}
