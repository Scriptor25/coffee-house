package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.media.AudioTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class AudioTrackJsonConverter : Converter<AudioTrack, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: AudioTrack): JsonNode = db { value.toJson() }
}
