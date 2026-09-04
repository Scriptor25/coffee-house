package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object FilterCapabilitiesTable : IdTable<FilterId>("filter_capabilities") {
    override val id = filterId("id").entityId()

    val transform = text("transform")
    val timelineSupport = bool("timeline_support")
    val sliceThreading = bool("slice_threading")
}

class FilterCapabilities(id: EntityID<FilterId>) : Entity<FilterId>(id) {
    companion object : EntityClass<FilterId, FilterCapabilities>(FilterCapabilitiesTable)

    var transform by FilterCapabilitiesTable.transform
    var timelineSupport by FilterCapabilitiesTable.timelineSupport
    var sliceThreading by FilterCapabilitiesTable.sliceThreading

    override fun toString(): String {
        return "FilterCapabilities(id=$id, transform=$transform, timelineSupport=$timelineSupport, sliceThreading=$sliceThreading)"
    }
}
