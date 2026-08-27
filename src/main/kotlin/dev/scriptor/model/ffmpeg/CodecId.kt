package dev.scriptor.model.ffmpeg

@JvmInline
value class CodecId(private val value: String) {
    override fun toString(): String = value
}
