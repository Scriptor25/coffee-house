package dev.scriptor.converter

import dev.scriptor.JsonArrayNode
import dev.scriptor.JsonNode
import dev.scriptor.jsonNull
import dev.scriptor.jsonOf
import dev.scriptor.reflect.getClass
import dev.scriptor.reflect.getType
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.converter.ConverterFn

class ListJsonConverter : Converter<List<*>, JsonArrayNode> {

    context(provider: Provider)
    override fun convert(value: List<*>): JsonArrayNode {
        return jsonOf(*value.map {
            if (it == null) jsonNull() else {
                val src = getClass(it::class).createType()
                val dst = getType<JsonNode>()

                val convert = provider[src to dst] as? ConverterFn<Any, JsonNode>
                    ?: error("unsupported conversion from $src to $dst")

                convert(it)
            }
        }.toTypedArray())
    }
}
