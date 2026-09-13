package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonIntConverter : Converter<JsonNode, Int> {

    context(provider: Provider)
    override fun convert(value: JsonNode): Int = value.fromJson<Number>().toInt()
}
