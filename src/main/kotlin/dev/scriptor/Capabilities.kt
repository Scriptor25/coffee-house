package dev.scriptor

data class FfmpegCapabilities(
    val version: String,
    val configuration: List<String>,

    val devices: Map<String, FfmpegDeviceCapabilities>,

    val encoders: Map<String, FfmpegCodecCapabilities>,
    val decoders: Map<String, FfmpegCodecCapabilities>,

    val filters: Map<String, FfmpegFilterCapabilities>,

    val interop: Map<FfmpegDevicePair, FfmpegInteropCapabilities>
)

data class FfmpegDeviceCapabilities(
    val type: String,

    val compiled: Boolean,
    val available: Boolean,

    val message: String?,

    val formats: Set<String>,

    val encoders: Set<String>,
    val decoders: Set<String>,

    val filters: Set<String>,
)

data class FfmpegCodecCapabilities(
    val type: String?,

    val name: String,
    val longName: String?,
    val description: String,

    val hardware: Boolean,

    val formats: Set<String>,
)

data class FfmpegFilterCapabilities(
    val name: String,
    val description: String,

    val help: String,
)

data class FfmpegDevicePair(
    val src: String,
    val dst: String,
)

data class FfmpegInteropCapabilities(
    val supportDeriving: Boolean,
    val supportDirectMapping: Boolean,
    val supportMapping: Boolean,

    val message: String?,
)
