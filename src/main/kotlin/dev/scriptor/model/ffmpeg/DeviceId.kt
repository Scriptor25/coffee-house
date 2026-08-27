package dev.scriptor.model.ffmpeg

@JvmInline
value class DeviceId(private val value: String) {
    override fun toString(): String = value
}
