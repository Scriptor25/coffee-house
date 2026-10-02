package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.media.SubtitleTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class SubtitleTrackJsonConverter : Converter<SubtitleTrack, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: SubtitleTrack): JsonNode = db { value.toJson() }
}
