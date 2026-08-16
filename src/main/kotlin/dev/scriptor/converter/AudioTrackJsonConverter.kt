package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.AudioTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class AudioTrackJsonConverter : Converter<AudioTrack, JsonNode> {

    context(provider: Provider)
    override fun convert(value: AudioTrack): JsonNode = jsonOf(
        "index" to jsonOf(value.index),
        "codec" to jsonOf(value.codec),
        "bit_rate" to jsonOf(value.bitRate),
        "sample_rate" to jsonOf(value.sampleRate),
        "channels" to jsonOf(value.channels),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
        "default" to jsonOf(value.default),
        "forced" to jsonOf(value.forced),
    )
}
