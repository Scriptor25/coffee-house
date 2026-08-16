package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Chapter
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class ChapterJsonConverter : Converter<Chapter, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Chapter): JsonNode = jsonOf(
        "index" to jsonOf(value.index),
        "start" to jsonOf(value.start),
        "end" to jsonOf(value.end),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
    )
}
