package dev.scriptor.model.ffmpeg

data class FilterCapabilities(
    val type: String,
    val name: String,
    val transform: String,
    val description: String,
)
