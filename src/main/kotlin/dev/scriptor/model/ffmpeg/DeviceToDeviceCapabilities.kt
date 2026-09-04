package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object DeviceToDeviceCapabilitiesTable : UuidTable("device_to_device_capabilities") {
    val src = reference("src", DeviceCapabilitiesTable, ReferenceOption.CASCADE)
    val dst = reference("dst", DeviceCapabilitiesTable, ReferenceOption.CASCADE)

    val derivable = bool("derivable")
    val direct = bool("direct")
    val mapping = bool("mapping")

    init {
        uniqueIndex(src, dst)
    }
}

class DeviceToDeviceCapabilities(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<DeviceToDeviceCapabilities>(DeviceToDeviceCapabilitiesTable)

    var src by DeviceCapabilities referencedOn DeviceToDeviceCapabilitiesTable.src
    var dst by DeviceCapabilities referencedOn DeviceToDeviceCapabilitiesTable.dst

    var derivable by DeviceToDeviceCapabilitiesTable.derivable
    var direct by DeviceToDeviceCapabilitiesTable.direct
    var mapping by DeviceToDeviceCapabilitiesTable.mapping

    override fun toString(): String {
        return "DeviceToDeviceCapabilities(id=$id, src=$src, dst=$dst, derivable=$derivable, direct=$direct, mapping=$mapping)"
    }
}
