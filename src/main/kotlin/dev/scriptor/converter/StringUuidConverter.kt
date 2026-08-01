package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.uuid.Uuid

class StringUuidConverter : Converter<String, Uuid> {

    context(provider: Provider)
    override fun convert(value: String): Uuid = Uuid.parseHexDash(value)
}
