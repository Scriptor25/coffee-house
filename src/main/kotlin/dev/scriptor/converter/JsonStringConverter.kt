package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonStringConverter : Converter<JsonNode, String> {

    context(provider: Provider)
    override fun convert(value: JsonNode) = value.toString()
}
