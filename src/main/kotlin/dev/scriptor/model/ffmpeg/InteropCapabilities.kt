package dev.scriptor.model.ffmpeg

data class InteropCapabilities(
    val src: DeviceId,
    val dst: DeviceId,

    val derivable: Boolean,
    val direct: Boolean,
    val mapping: Boolean,
)
