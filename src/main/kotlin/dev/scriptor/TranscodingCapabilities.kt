package dev.scriptor

data class TranscodingCapabilities(
    val devices: Set<String>,
    val decoders: List<CodecCapabilities>,
    val encoders: List<CodecCapabilities>,
)
