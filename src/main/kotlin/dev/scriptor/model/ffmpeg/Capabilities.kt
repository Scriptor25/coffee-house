package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

data object Capabilities {

    fun getSoftwareDecoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.DECODE)
                        and (ImplementationTable.kind eq ImplementationKind.SOFTWARE)
            )
            .toList()
    }

    fun getSoftwareEncoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.ENCODE)
                        and (ImplementationTable.kind eq ImplementationKind.SOFTWARE)
            )
            .toList()
    }

    fun getHybridDecoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.DECODE)
                        and (ImplementationTable.kind eq ImplementationKind.HYBRID)
            )
            .toList()
    }

    fun getHybridEncoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.ENCODE)
                        and (ImplementationTable.kind eq ImplementationKind.HYBRID)
            )
            .toList()
    }

    fun getHardwareDecoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.DECODE)
                        and (ImplementationTable.kind eq ImplementationKind.HARDWARE)
            )
            .toList()
    }

    fun getHardwareEncoders(): List<Implementation> {
        return Implementation
            .find(
                (ImplementationTable.direction eq ImplementationDirection.ENCODE)
                        and (ImplementationTable.kind eq ImplementationKind.HARDWARE)
            )
            .toList()
    }

    fun getDeviceDecoders(device: Device): List<Implementation> {
        return device.implementations
            .filter { it.direction == ImplementationDirection.DECODE }
    }

    fun getDeviceEncoders(device: Device): List<Implementation> {
        return device.implementations
            .filter { it.direction == ImplementationDirection.ENCODE }
    }

    fun getCodecDecoders(codec: Codec): List<Implementation> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.DECODE }
    }

    fun getCodecEncoders(codec: Codec): List<Implementation> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.ENCODE }
    }

    fun getDeviceToDevice(src: Device, dst: Device): DeviceToDevice? {
        return DeviceToDevice
            .find((DeviceToDeviceTable.src eq src.id) and (DeviceToDeviceTable.dst eq dst.id))
            .firstOrNull()
    }

    fun getDevicesForDecoding(codec: Codec): Set<Device> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.DECODE }
            .flatMap { it.devices }
            .toSet()
    }

    fun getDevicesForEncoding(codec: Codec): Set<Device> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.ENCODE }
            .flatMap { it.devices }
            .toSet()
    }

    fun getDecoders(codec: Codec, device: Device?): Set<Implementation> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.DECODE }
            .filter {
                when (device) {
                    null -> it.kind == ImplementationKind.SOFTWARE
                    else -> device in it.devices
                }
            }
            .toSet()
    }

    fun getEncoders(codec: Codec, device: Device?): Set<Implementation> {
        return codec.implementations
            .filter { it.direction == ImplementationDirection.ENCODE }
            .filter {
                when (device) {
                    null -> it.kind == ImplementationKind.SOFTWARE
                    else -> device in it.devices
                }
            }
            .toSet()
    }

    fun compare(a: Device, b: Device): Int {
        val a = DeviceBackend.find(a.id.value) ?: return -1
        val b = DeviceBackend.find(b.id.value) ?: return 1

        return a.priority - b.priority
    }

    fun compare(a: Implementation, b: Implementation): Int {
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
