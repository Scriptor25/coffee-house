package dev.scriptor.model.ffmpeg

import dev.scriptor.JsonArrayNode
import dev.scriptor.JsonNumberNode
import dev.scriptor.jsonArray
import dev.scriptor.jsonOf
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object FormatTable : IdTable<FormatId>("format") {
    override val id = formatId("id").entityId()

    val components = json<List<Int>>(
        "components",
        from = {
            if (it !is JsonArrayNode) {
                error("invalid node")
            }

            it.map { node ->
                if (node !is JsonNumberNode) {
                    error("invalid node '[N]'")
                }

                node.value.toInt()
            }
        },
        to = { jsonArray { it.forEach { entry -> add(jsonOf(entry)) } } },
    )

    val bitsPerPixel = integer("bits_per_pixel")

    val input = bool("input")
    val output = bool("output")
    val hardware = bool("hardware")
    val paletted = bool("paletted")
    val bitstream = bool("bitstream")
}

class Format(id: EntityID<FormatId>) : Entity<FormatId>(id) {
    companion object : EntityClass<FormatId, Format>(FormatTable)

    var components by FormatTable.components
    var bitsPerPixel by FormatTable.bitsPerPixel
    var input by FormatTable.input
    var output by FormatTable.output
    var hardware by FormatTable.hardware
    var paletted by FormatTable.paletted
    var bitstream by FormatTable.bitstream

    override fun toString(): String {
        return "Format(id=$id, components=$components, bitsPerPixel=$bitsPerPixel, input=$input, output=$output, hardware=$hardware, paletted=$paletted, bitstream=$bitstream)"
    }
}
