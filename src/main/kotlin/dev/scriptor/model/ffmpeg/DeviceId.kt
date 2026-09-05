package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class DeviceIdColumnType : ColumnType<DeviceId>() {
    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): DeviceId? {
        return when (value) {
            is DeviceId -> value
            is String -> DeviceId(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.deviceId(name: String): Column<DeviceId> = registerColumn(name, DeviceIdColumnType())

@JvmInline
value class DeviceId(private val value: String) {
    override fun toString(): String = value
}
