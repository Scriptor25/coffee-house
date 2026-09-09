package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.media.VideoTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class VideoTrackJsonConverter : Converter<VideoTrack, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: VideoTrack): JsonObjectNode = jsonOf(
        "index" to jsonOf(value.index),
        "codec" to jsonOf(value.codec.id.toString()),
        "width" to jsonOf(value.width),
        "height" to jsonOf(value.height),
        "bit_rate" to jsonOf(value.bitRate),
        "frame_rate" to jsonOf(value.frameRate),
        "profile" to jsonOf(value.profile),
        "level" to jsonOf(value.level),
        "hdr" to jsonOf(value.hdr),
        "language" to jsonOf(value.language),
        "title" to jsonOf(value.title),
        "default" to jsonOf(value.default),
    )
}
