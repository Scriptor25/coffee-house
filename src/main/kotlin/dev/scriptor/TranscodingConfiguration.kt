package dev.scriptor

import dev.scriptor.codec.AudioCodec
import dev.scriptor.codec.SubtitleCodec
import dev.scriptor.codec.VideoCodec

data class TranscodingConfiguration(
    val enable: Boolean,
    val video: VideoCodec,
    val audio: AudioCodec,
    val subtitle: SubtitleCodec,
    val pipeline: Pipeline,
)
