package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.context.Configuration
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class ConfigurationJsonConverter : Converter<Configuration, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Configuration): JsonNode = value.toJson()
}
