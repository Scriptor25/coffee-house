package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

data object Capabilities {

    fun getSoftwareDecoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.SOFTWARE)
            )
            .toList()
    }

    fun getSoftwareEncoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.SOFTWARE)
            )
            .toList()
    }

    fun getHybridDecoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.HYBRID)
            )
            .toList()
    }

    fun getHybridEncoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.HYBRID)
            )
            .toList()
    }

    fun getHardwareDecoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.HARDWARE)
            )
            .toList()
    }

    fun getHardwareEncoders(): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.kind eq ImplementationKind.HARDWARE)
            )
            .toList()
    }

    fun getDeviceDecoders(device: DeviceId): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.id eq ImplementationDeviceTable.implementation)
                        and (ImplementationDeviceTable.device eq device)
            )
            .toList()
    }

    fun getDeviceEncoders(device: DeviceId): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.id eq ImplementationDeviceTable.implementation)
                        and (ImplementationDeviceTable.device eq device)
            )
            .toList()
    }

    fun getCodecDecoders(codec: CodecId): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
            )
            .toList()
    }

    fun getCodecEncoders(codec: CodecId): List<ImplementationCapabilities> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
            )
            .toList()
    }

    fun getDeviceToDevice(src: DeviceId, dst: DeviceId): DeviceToDeviceCapabilities? {
        return DeviceToDeviceCapabilities
            .find(
                (DeviceToDeviceCapabilitiesTable.src eq src)
                        and (DeviceToDeviceCapabilitiesTable.dst eq dst)
            )
            .firstOrNull()
    }

    fun getDevicesForDecoding(codec: CodecId): Set<DeviceId> {
        // TODO: check if device is available
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
            )
            .flatMap { it.supportedHardwareDevices }
            .map { it.id.value }
            .toSet()
    }

    fun getDevicesForEncoding(codec: CodecId): Set<DeviceId> {
        // TODO: check if device is available
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
            )
            .flatMap { it.supportedHardwareDevices }
            .map { it.id.value }
            .toSet()
    }

    fun getDecoders(codec: CodecId, device: DeviceId?): Set<ImplementationId> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.DECODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
                        and
                        when (device) {
                            null -> (ImplementationCapabilitiesTable.kind eq ImplementationKind.SOFTWARE)
                            else -> (
                                    (ImplementationCapabilitiesTable.id eq ImplementationDeviceTable.implementation)
                                            and (ImplementationDeviceTable.device eq device)
                                    )
                        }
            )
            .map { it.id.value }
            .toSet()
    }

    fun getEncoders(codec: CodecId, device: DeviceId?): Set<ImplementationId> {
        return ImplementationCapabilities
            .find(
                (ImplementationCapabilitiesTable.direction eq CodecDirection.ENCODE)
                        and (ImplementationCapabilitiesTable.codec eq codec)
                        and
                        when (device) {
                            null -> (ImplementationCapabilitiesTable.kind eq ImplementationKind.SOFTWARE)
                            else -> (
                                    (ImplementationCapabilitiesTable.id eq ImplementationDeviceTable.implementation)
                                            and (ImplementationDeviceTable.device eq device)
                                    )
                        }
            )
            .map { it.id.value }
            .toSet()
    }

    fun compare(a: DeviceId, b: DeviceId): Int {
        val a = DeviceBackend.find(a) ?: return -1
        val b = DeviceBackend.find(b) ?: return 1

        return a.priority - b.priority
    }

    fun compare(a: ImplementationId, b: ImplementationId): Int {
        val a = ImplementationCapabilities.findById(a) ?: return -1
        val b = ImplementationCapabilities.findById(b) ?: return 1

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
