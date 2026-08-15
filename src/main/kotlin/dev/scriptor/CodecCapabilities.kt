package dev.scriptor

data class CodecCapabilities(
    val name: String,
    val type: CodecType,
    val frameLevelMultithreading: Boolean,
    val sliceLevelMultithreading: Boolean,
    val experimental: Boolean,
    val supportDrawHorizontalBand: Boolean,
    val supportDirectRendering: Boolean,
    val description: String,
)
