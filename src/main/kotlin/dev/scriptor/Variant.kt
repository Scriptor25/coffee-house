package dev.scriptor

data class Variant(
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Long,
    val profile: Profile,
)
