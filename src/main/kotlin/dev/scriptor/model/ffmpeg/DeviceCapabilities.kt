package dev.scriptor.model.ffmpeg

data class DeviceCapabilities(
    val type: String,
    val encoders: Set<String>,
    val decoders: Set<String>,
    val filters: Set<String>,
)
