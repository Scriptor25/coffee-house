package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.media.AudioTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class AudioTrackJsonConverter : Converter<AudioTrack, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: AudioTrack): JsonObjectNode = jsonOf(
        "index" to jsonOf(value.index),
        "codec" to jsonOf(value.codec.id.toString()),
        "bit_rate" to jsonOf(value.bitRate),
        "sample_rate" to jsonOf(value.sampleRate),
        "channels" to jsonOf(value.channels),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
        "default" to jsonOf(value.default),
        "forced" to jsonOf(value.forced),
    )
}
