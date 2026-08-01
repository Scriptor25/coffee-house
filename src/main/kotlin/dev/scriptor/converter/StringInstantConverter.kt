package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.time.Instant

class StringInstantConverter : Converter<String, Instant> {

    context(provider: Provider)
    override fun convert(value: String) = Instant.parse(value)
}
