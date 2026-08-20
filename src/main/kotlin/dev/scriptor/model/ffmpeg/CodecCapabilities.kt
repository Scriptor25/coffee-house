package dev.scriptor.model.ffmpeg

data class CodecCapabilities(
    val id: CodecId,
    val type: CodecType,

    val supportsDecoding: Boolean,
    val supportsEncoding: Boolean,

    val intraFrameOnly: Boolean,
    val lossyCompression: Boolean,
    val losslessCompression: Boolean,

    val decoders: Set<ImplementationId>,
    val encoders: Set<ImplementationId>,
)
