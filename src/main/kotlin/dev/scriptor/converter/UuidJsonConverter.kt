package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson
import kotlin.uuid.Uuid

class UuidJsonConverter : Converter<Uuid, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Uuid): JsonNode = value.toHexDashString().toJson()
}
