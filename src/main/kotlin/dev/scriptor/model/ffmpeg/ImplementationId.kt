package dev.scriptor.model.ffmpeg

@JvmInline
value class ImplementationId(private val value: String) {
    override fun toString(): String = value
}
