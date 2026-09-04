package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object DeviceCapabilitiesTable : IdTable<DeviceId>("device_capabilities") {
    override val id = deviceId("id").entityId()
}

class DeviceCapabilities(id: EntityID<DeviceId>) : Entity<DeviceId>(id) {
    companion object : EntityClass<DeviceId, DeviceCapabilities>(DeviceCapabilitiesTable)

    val implementations by ImplementationCapabilities via ImplementationDeviceTable
}
