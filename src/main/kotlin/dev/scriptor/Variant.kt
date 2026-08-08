package dev.scriptor

enum class Profile(
    val preset: String,
    val crf: Int,
) {
    LOSSLESS("veryslow", 18),
    HIGH("slow", 20),
    MEDIUM("medium", 22),
    LOW("veryfast", 24),
    POTATO("superfast", 26),
}

interface Variant {
    val name: String
}

class OriginalVariant : Variant {
    override val name: String = "original"
}

data class ScaleVariant(
    override val name: String,

    val width: Int,
    val height: Int,
    val bitrate: Long,
    val profile: Profile,
) : Variant
