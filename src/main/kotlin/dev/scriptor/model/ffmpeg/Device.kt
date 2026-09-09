package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object DeviceTable : IdTable<DeviceId>("device") {
    override val id = deviceId("id").entityId()
}

class Device(id: EntityID<DeviceId>) : Entity<DeviceId>(id) {
    companion object : EntityClass<DeviceId, Device>(DeviceTable)

    val implementations by Implementation via ImplementationDeviceTable
}
