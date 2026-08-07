package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.ConversionPath
import dev.scriptor.server.converter.Converter
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

class ListJsonConverter : Converter<List<*>, JsonNode> {

    context(provider: Provider)
    override fun convert(value: List<*>): JsonNode {
        return jsonOf(*value.map {
            if (it == null) jsonOf(null) else {
                val type = it::class.starProjectedType
                val converter = provider[type to typeOf<JsonNode>()]!! as ConversionPath<Any, JsonNode>

                converter(it)
            }
        }.toTypedArray())
    }
}
