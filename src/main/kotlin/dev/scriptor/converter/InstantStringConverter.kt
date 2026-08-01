package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.time.Instant

class InstantStringConverter : Converter<Instant, String> {

    context(provider: Provider)
    override fun convert(value: Instant) = value.toString()
}
