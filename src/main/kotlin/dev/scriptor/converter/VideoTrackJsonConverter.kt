package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.media.VideoTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class VideoTrackJsonConverter : Converter<VideoTrack, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: VideoTrack): JsonNode = db { value.toJson() }
}
