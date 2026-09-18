package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.model.CreatePlaybackBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonCreatePlaybackBodyConverter : Converter<JsonNode, CreatePlaybackBody> {

    context(provider: Provider?)
    override fun convert(value: JsonNode): CreatePlaybackBody = value.fromJson()
}
