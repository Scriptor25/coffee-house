package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class FilterIdColumnType : ColumnType<FilterId>() {
    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): FilterId? {
        return when (value) {
            is FilterId -> value
            is String -> FilterId(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.filterId(name: String): Column<FilterId> = registerColumn(name, FilterIdColumnType())

@JvmInline
value class FilterId(private val value: String) {
    override fun toString(): String = value
}
