package dev.scriptor.model.ffmpeg

data class Capabilities(
    val devices: Map<DeviceId, DeviceCapabilities>,
    val codecs: Map<CodecId, CodecCapabilities>,
    val decoders: Map<ImplementationId, ImplementationCapabilities>,
    val encoders: Map<ImplementationId, ImplementationCapabilities>,
    val filters: Map<FilterId, FilterCapabilities>,
    val interop: Map<Pair<DeviceId, DeviceId>, InteropCapabilities>,
) {
    fun getDevice(id: DeviceId): DeviceCapabilities? {
        return devices[id]
    }

    fun getDeviceDecoders(id: DeviceId): Map<ImplementationId, ImplementationCapabilities> {
        val device = devices[id] ?: return emptyMap()
        return decoders.filterValues { device.id in it.supportedHardwareDevices }
    }

    fun getDeviceEncoders(id: DeviceId): Map<ImplementationId, ImplementationCapabilities> {
        val device = devices[id] ?: return emptyMap()
        return encoders.filterValues { device.id in it.supportedHardwareDevices }
    }

    fun getCodecDecoders(id: CodecId): Map<ImplementationId, ImplementationCapabilities> {
        val codec = codecs[id] ?: return emptyMap()
        return codec.decoders
            .mapNotNull(decoders::get)
            .associateBy(ImplementationCapabilities::id)
    }

    fun getCodecEncoders(id: CodecId): Map<ImplementationId, ImplementationCapabilities> {
        val codec = codecs[id] ?: return emptyMap()
        return codec.encoders
            .mapNotNull(encoders::get)
            .associateBy(ImplementationCapabilities::id)
    }

    fun getInterop(src: DeviceId, dst: DeviceId): InteropCapabilities? {
        val src = devices[src] ?: return null
        val dst = devices[dst] ?: return null
        return interop[src.id to dst.id]
    }

    fun getDevicesForDecoding(codec: CodecId): Set<DeviceId> {
        val codec = codecs[codec] ?: return emptySet()

        return codec.decoders
            .mapNotNull(decoders::get)
            .flatMap(ImplementationCapabilities::supportedHardwareDevices)
            .toSet()
    }

    fun getDevicesForEncoding(codec: CodecId): Set<DeviceId> {
        val codec = codecs[codec] ?: return emptySet()

        return codec.encoders
            .mapNotNull(encoders::get)
            .flatMap(ImplementationCapabilities::supportedHardwareDevices)
            .toSet()
    }
}
