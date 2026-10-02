package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.uuid.Uuid

class JsonUuidConverter : Converter<JsonNode, Uuid> {

    context(provider: Provider?)
    override fun convert(value: JsonNode): Uuid = Uuid.parse(value.fromJson())
}
