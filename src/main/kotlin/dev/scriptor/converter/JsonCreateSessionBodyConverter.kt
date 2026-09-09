package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.CreateSessionBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonCreateSessionBodyConverter : Converter<JsonNode, CreateSessionBody> {

    context(provider: Provider)
    override fun convert(value: JsonNode): CreateSessionBody = value.cast()
}
