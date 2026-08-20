package dev.scriptor

data class TranscodingRequirements(
    val enable: Boolean,
    val device: String?,
    val video: String,
    val audio: String,
    val subtitle: String,
)
