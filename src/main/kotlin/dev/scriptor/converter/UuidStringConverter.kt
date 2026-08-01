package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.uuid.Uuid

class UuidStringConverter : Converter<Uuid, String> {

    context(provider: Provider)
    override fun convert(value: Uuid): String = value.toHexDashString()
}
