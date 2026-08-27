package dev.scriptor.model.ffmpeg

@JvmInline
value class PixelFormat(private val value: String) {
    override fun toString(): String = value
}
