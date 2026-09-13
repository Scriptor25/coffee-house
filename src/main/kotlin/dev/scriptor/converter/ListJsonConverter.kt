package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class ListJsonConverter : Converter<List<*>, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: List<*>): JsonNode = value.toJson()
}
