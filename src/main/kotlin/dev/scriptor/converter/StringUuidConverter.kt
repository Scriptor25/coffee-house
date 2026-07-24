package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import kotlin.uuid.Uuid

class StringUuidConverter : Converter<String, Uuid> {

    override fun convert(value: String): Uuid = Uuid.parseHexDash(value)
}