package dev.scriptor.model.ffmpeg

data class InteropCapabilities(
    val src: String,
    val dst: String,

    val derivable: Boolean,
    val direct: Boolean,
    val mapping: Boolean,
)
