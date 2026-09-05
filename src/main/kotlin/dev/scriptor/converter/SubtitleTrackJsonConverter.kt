package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.media.SubtitleTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class SubtitleTrackJsonConverter : Converter<SubtitleTrack, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: SubtitleTrack): JsonObjectNode = jsonOf(
        "index" to jsonOf(value.index),
        "codec" to jsonOf(value.codec.id.toString()),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
        "default" to jsonOf(value.default),
        "forced" to jsonOf(value.forced),
    )
}
