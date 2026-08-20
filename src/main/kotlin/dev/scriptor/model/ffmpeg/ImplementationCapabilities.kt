package dev.scriptor.model.ffmpeg

data class ImplementationCapabilities(
    val id: ImplementationId,

    val type: CodecType,
    val direction: CodecDirection,

    val frameLevelMultithreading: Boolean,
    val sliceLevelMultithreading: Boolean,
    val experimental: Boolean,
    val supportDrawHorizontalBand: Boolean,
    val supportDirectRendering: Boolean,

    val generalCapabilities: Set<String> = emptySet(),
    val threadingCapabilities: Set<String> = emptySet(),

    val supportedHardwareDevices: Set<DeviceId> = emptySet(),

    val supportedPixelFormats: Set<String> = emptySet(),

    val supportedSampleRates: Set<Long> = emptySet(),
    val supportedSampleFormats: Set<String> = emptySet(),
    val supportedChannelLayouts: Set<String> = emptySet(),
)
