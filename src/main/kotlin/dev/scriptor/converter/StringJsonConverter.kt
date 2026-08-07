package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringJsonConverter : Converter<String, JsonNode> {

    context(provider: Provider)
    override fun convert(value: String): JsonNode = TODO("parse json string")
}
