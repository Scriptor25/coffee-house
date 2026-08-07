package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.SubtitleTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class SubtitleTrackJsonConverter : Converter<SubtitleTrack, JsonNode> {

    context(provider: Provider)
    override fun convert(value: SubtitleTrack): JsonNode = jsonOf(
        "index" to jsonOf(value.index),
        "codec" to jsonOf(value.codec),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
        "default" to jsonOf(value.default),
        "forced" to jsonOf(value.forced),
    )
}
