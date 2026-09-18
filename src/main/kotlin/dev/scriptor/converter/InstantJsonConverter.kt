package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.time.Instant

class InstantJsonConverter : Converter<Instant, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Instant): JsonNode = jsonOf(value.toString())
}
