package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.reflect.typeOf

class JsonOffsetLimitConverter : Converter<JsonNode, OffsetLimitBody> {

    context(provider: Provider)
    override fun convert(value: JsonNode): OffsetLimitBody = value.cast(
        mapOf(
            typeOf<Int>() to { it.cast<Number>().toInt() },
            typeOf<Long>() to { it.cast<Number>().toLong() },
        ),
    )
}
