package dev.scriptor.model.ffmpeg

data class Capabilities(
    val devices: Map<DeviceId, DeviceCapabilities>,
    val interop: Map<Pair<DeviceId, DeviceId>, InteropCapabilities>,
    val implementations: Map<ImplementationId, ImplementationCapabilities>,
    val codecs: Map<CodecId, CodecCapabilities>,
    val filters: Map<FilterId, FilterCapabilities>,
) {
    val decoders = implementations.filterValues { it.direction == CodecDirection.DECODE }
    val encoders = implementations.filterValues { it.direction == CodecDirection.ENCODE }

    fun getDevice(id: DeviceId): DeviceCapabilities? {
        return devices[id]
    }

    fun getSoftwareDecoders(): Map<ImplementationId, ImplementationCapabilities> {
        return decoders.filterValues { it.kind == ImplementationKind.SOFTWARE }
    }

    fun getSoftwareEncoders(): Map<ImplementationId, ImplementationCapabilities> {
        return encoders.filterValues { it.kind == ImplementationKind.SOFTWARE }
    }

    fun getHybridDecoders(): Map<ImplementationId, ImplementationCapabilities> {
        return decoders.filterValues { it.kind == ImplementationKind.HYBRID }
    }

    fun getHybridEncoders(): Map<ImplementationId, ImplementationCapabilities> {
        return encoders.filterValues { it.kind == ImplementationKind.HYBRID }
    }

    fun getHardwareDecoders(): Map<ImplementationId, ImplementationCapabilities> {
        return decoders.filterValues { it.kind == ImplementationKind.HARDWARE }
    }

    fun getHardwareEncoders(): Map<ImplementationId, ImplementationCapabilities> {
        return encoders.filterValues { it.kind == ImplementationKind.HARDWARE }
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
            .filter(devices::containsKey)
            .toSet()
    }

    fun getDevicesForEncoding(codec: CodecId): Set<DeviceId> {
        val codec = codecs[codec] ?: return emptySet()

        return codec.encoders
            .mapNotNull(encoders::get)
            .flatMap(ImplementationCapabilities::supportedHardwareDevices)
            .filter(devices::containsKey)
            .toSet()
    }

    fun getDecoders(codec: CodecId, device: DeviceId?): Set<ImplementationId> {
        val codec = codecs[codec] ?: return emptySet()

        return codec.decoders
            .mapNotNull(decoders::get)
            .filter {
                if (device == null) it.kind == ImplementationKind.SOFTWARE
                else device in it.supportedHardwareDevices
            }
            .map(ImplementationCapabilities::id)
            .toSet()
    }

    fun getEncoders(codec: CodecId, device: DeviceId?): Set<ImplementationId> {
        val codec = codecs[codec] ?: return emptySet()

        return codec.encoders
            .mapNotNull(encoders::get)
            .filter {
                if (device == null) it.kind == ImplementationKind.SOFTWARE
                else device in it.supportedHardwareDevices
            }
            .map(ImplementationCapabilities::id)
            .toSet()
    }

    fun compare(a: DeviceId, b: DeviceId): Int {
        val a = DeviceBackend.find(a) ?: return -1
        val b = DeviceBackend.find(b) ?: return 1

        return a.priority - b.priority
    }

    fun compare(a: ImplementationId, b: ImplementationId): Int {
        val a = implementations[a] ?: return -1
        val b = implementations[b] ?: return 1

        var errorA = 0
        var errorB = 0

        if (a.experimental) {
            errorA += 1
        }
        if (b.experimental) {
            errorB += 1
        }

        if (a.kind == ImplementationKind.HYBRID) {
            errorA += 1
        }
        if (b.kind == ImplementationKind.HYBRID) {
            errorB += 1
        }

        return errorA - errorB
    }
}
