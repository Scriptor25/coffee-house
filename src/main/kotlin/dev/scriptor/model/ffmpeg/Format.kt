package dev.scriptor.model.ffmpeg

import dev.scriptor.fromJsonNoContext
import dev.scriptor.json
import dev.scriptor.toJsonNoContext
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object FormatTable : IdTable<FormatId>("format") {
    override val id = formatId("id").entityId()

    val components = json<List<Int>>(
        "components",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
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
