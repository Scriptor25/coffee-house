package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringBooleanConverter : Converter<String, Boolean?> {

    context(provider: Provider)
    override fun convert(value: String): Boolean = value == "true"
}