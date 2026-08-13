package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.reflect.getClass
import dev.scriptor.reflect.getType
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.converter.ConverterFn

class ListJsonConverter : Converter<List<*>, JsonNode> {

    context(provider: Provider)
    override fun convert(value: List<*>): JsonNode {
        return jsonOf(*value.map {
            if (it == null) jsonOf(null) else {
                val src = getClass(it::class).createType()
                val dst = getType<JsonNode>()

                val convert = provider[src to dst] as? ConverterFn<Any, JsonNode>
                    ?: error("unsupported conversion from $src to $dst")

                convert(it)
            }
        }.toTypedArray())
    }
}
