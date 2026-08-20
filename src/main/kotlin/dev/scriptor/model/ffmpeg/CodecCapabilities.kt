package dev.scriptor.model.ffmpeg

data class CodecCapabilities(
    val name: String,
    val description: String,

    val type: CodecType,
    val direction: CodecDirection,

    val frameLevelMultithreading: Boolean,
    val sliceLevelMultithreading: Boolean,
    val experimental: Boolean,
    val supportDrawHorizontalBand: Boolean,
    val supportDirectRendering: Boolean,

    val generalCapabilities: Set<String> = emptySet(),
    val threadingCapabilities: Set<String> = emptySet(),

    val pixelFormats: Set<String> = emptySet(),

    val sampleRates: Set<Long> = emptySet(),
    val sampleFormats: Set<String> = emptySet(),
    val channelLayouts: Set<String> = emptySet(),
)
