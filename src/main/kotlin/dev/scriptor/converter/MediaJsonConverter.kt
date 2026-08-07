package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class MediaJsonConverter : Converter<Media, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Media): JsonNode {
        return jsonOf(
            "id" to jsonOf(value.id),
            "path" to jsonOf(value.path),
            "title" to jsonOf(value.title),
            "created_at" to jsonOf(value.createdAt),
            "modified_at" to jsonOf(value.modifiedAt),
        )
    }
}
