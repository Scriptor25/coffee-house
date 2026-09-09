package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class FormatIdColumnType : ColumnType<FormatId>() {
    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): FormatId? {
        return when (value) {
            is FormatId -> value
            is String -> FormatId(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.formatId(name: String): Column<FormatId> = registerColumn(name, FormatIdColumnType())

@JvmInline
value class FormatId(private val value: String) {
    override fun toString(): String = value
}
