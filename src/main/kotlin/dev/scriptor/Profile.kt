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
