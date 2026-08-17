package dev.scriptor

import dev.scriptor.codec.AudioCodec
import dev.scriptor.codec.SubtitleCodec
import dev.scriptor.codec.VideoCodec

data class TranscodingRequirements(
    val enable: Boolean,
    val device: String?,
    val video: VideoCodec,
    val audio: AudioCodec,
    val subtitle: SubtitleCodec,
)
