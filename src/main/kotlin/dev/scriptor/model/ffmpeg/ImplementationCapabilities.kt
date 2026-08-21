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

    val kind: ImplementationKind,

    val generalCapabilities: Set<String> = emptySet(),

    val supportedHardwareDevices: Set<DeviceId> = emptySet(),

    val supportedPixelFormats: Set<PixelFormat> = emptySet(),

    val supportedSampleRates: Set<Long> = emptySet(),
    val supportedSampleFormats: Set<String> = emptySet(),
    val supportedChannelLayouts: Set<String> = emptySet(),
)
