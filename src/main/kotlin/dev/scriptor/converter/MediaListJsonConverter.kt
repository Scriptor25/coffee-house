package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter

class MediaListJsonConverter : Converter<List<Media>, JsonNode> {

    context(provider: Provider)
    override fun convert(value: List<Media>): JsonNode {
        val converter = provider.convert<Media, JsonNode>()!!

        return jsonOf(*value.map { converter(it) }.toTypedArray())
    }
}
