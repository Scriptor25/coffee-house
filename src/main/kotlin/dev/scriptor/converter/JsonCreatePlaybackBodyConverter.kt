package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.CreatePlaybackBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid

class JsonCreatePlaybackBodyConverter : Converter<JsonNode, CreatePlaybackBody> {

    context(provider: Provider)
    override fun convert(value: JsonNode): CreatePlaybackBody = value.cast(
        mapOf(
            typeOf<Uuid>() to { Uuid.parseHexDash(it.cast<String>()) },
        ),
    )
}
