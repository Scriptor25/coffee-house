package dev.scriptor.model.ffmpeg

data class Capabilities(
    val devices: List<DeviceCapabilities>,
    val encoders: List<CodecCapabilities>,
    val decoders: List<CodecCapabilities>,
    val filters: List<FilterCapabilities>,
    val interop: List<InteropCapabilities>
)
