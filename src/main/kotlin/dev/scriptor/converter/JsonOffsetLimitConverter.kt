package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.OffsetLimit
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.reflect.typeOf

class JsonOffsetLimitConverter : Converter<JsonNode, OffsetLimit> {

    context(provider: Provider)
    override fun convert(value: JsonNode): OffsetLimit = value.cast(
        mapOf(
            typeOf<Int>() to { it.cast<Number>().toInt() },
            typeOf<Long>() to { it.cast<Number>().toLong() },
        ),
    )
}
