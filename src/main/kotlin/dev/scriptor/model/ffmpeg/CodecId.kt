package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class CodecIdColumnType : ColumnType<CodecId>() {
    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): CodecId? {
        return when (value) {
            is CodecId -> value
            is String -> CodecId(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.codecId(name: String): Column<CodecId> = registerColumn(name, CodecIdColumnType())

@JvmInline
value class CodecId(private val value: String) {
    override fun toString(): String = value
}
