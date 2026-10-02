package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.other.Other
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class OtherJsonConverter : Converter<Other, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Other): JsonNode = db { value.toJson() }
}
