package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.media.Chapter
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class ChapterJsonConverter : Converter<Chapter, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Chapter): JsonNode = db { value.toJson() }
}
