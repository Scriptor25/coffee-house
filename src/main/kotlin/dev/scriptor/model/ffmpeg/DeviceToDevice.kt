package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object DeviceToDeviceTable : UuidTable("device_to_device") {
    val src = reference("src", DeviceTable, ReferenceOption.CASCADE)
    val dst = reference("dst", DeviceTable, ReferenceOption.CASCADE)

    val derivable = bool("derivable")
    val direct = bool("direct")
    val mapping = bool("mapping")

    init {
        uniqueIndex(src, dst)
    }
}

class DeviceToDevice(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<DeviceToDevice>(DeviceToDeviceTable)

    var src by Device referencedOn DeviceToDeviceTable.src
    var dst by Device referencedOn DeviceToDeviceTable.dst

    var derivable by DeviceToDeviceTable.derivable
    var direct by DeviceToDeviceTable.direct
    var mapping by DeviceToDeviceTable.mapping

    override fun toString(): String {
        return "DeviceToDevice(id=$id, src=$src, dst=$dst, derivable=$derivable, direct=$direct, mapping=$mapping)"
    }
}
