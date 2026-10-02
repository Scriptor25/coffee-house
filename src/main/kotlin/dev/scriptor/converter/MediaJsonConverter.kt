package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.media.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class MediaJsonConverter : Converter<Media, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Media): JsonNode = db { value.toJson() }
}
