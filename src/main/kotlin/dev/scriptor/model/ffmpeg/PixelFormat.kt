package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class PixelFormatColumnType : ColumnType<PixelFormat>() {
    override var nullable: Boolean = false

    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): PixelFormat? {
        return when (value) {
            is PixelFormat -> value
            is String -> PixelFormat(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.pixelFormat(name: String): Column<PixelFormat> = registerColumn(name, PixelFormatColumnType())

@JvmInline
value class PixelFormat(private val value: String) {
    override fun toString(): String = value
}
