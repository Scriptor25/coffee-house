package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

class ImplementationIdColumnType : ColumnType<ImplementationId>() {
    override var nullable: Boolean = false

    override fun sqlType(): String {
        return "TEXT"
    }

    override fun valueFromDB(value: Any): ImplementationId? {
        return when (value) {
            is ImplementationId -> value
            is String -> ImplementationId(value)
            else -> error("unexpected value of type ${value::class}")
        }
    }
}

fun Table.implementationId(name: String): Column<ImplementationId> = registerColumn(name, ImplementationIdColumnType())

@JvmInline
value class ImplementationId(private val value: String) {
    override fun toString(): String = value
}
