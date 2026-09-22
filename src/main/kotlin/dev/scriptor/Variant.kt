package dev.scriptor

enum class Profile {
    ARCHIVAL,
    HIGH,
    MEDIUM,
    LOW,
    POTATO,
}

sealed interface Variant {
    val name: String
}

data object OriginalVariant : Variant {
    override val name: String = "original"
}

data class ScaleVariant(
    override val name: String,

    val width: Int,
    val height: Int,
    val bitrate: Long,
    val profile: Profile,
) : Variant
