package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson
import org.jetbrains.exposed.v1.jdbc.SizedIterable

class SizedIterableJsonConverter : Converter<SizedIterable<*>, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: SizedIterable<*>): JsonNode = db { value.toList().toJson() }
}
