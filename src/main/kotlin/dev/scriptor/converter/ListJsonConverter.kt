package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter

class ListJsonConverter : Converter<List<*>, JsonNode> {

    context(provider: Provider)
    override fun convert(value: List<*>): JsonNode {
        val converter = provider.convert<Any?, JsonNode>()!!

        return jsonOf(*value.map { converter(it) }.toTypedArray())
    }
}
