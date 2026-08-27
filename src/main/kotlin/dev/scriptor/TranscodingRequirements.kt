package dev.scriptor

import dev.scriptor.model.ffmpeg.CodecId

data class TranscodingRequirements(
    val enable: Boolean,
    val device: String?,
    val video: CodecId,
    val audio: CodecId,
    val subtitle: CodecId,
)
