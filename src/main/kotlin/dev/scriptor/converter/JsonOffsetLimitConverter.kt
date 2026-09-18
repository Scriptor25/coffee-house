package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.model.OffsetLimitBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonOffsetLimitConverter : Converter<JsonNode, OffsetLimitBody> {

    context(provider: Provider?)
    override fun convert(value: JsonNode): OffsetLimitBody = value.fromJson()
}
