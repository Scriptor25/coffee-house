package dev.scriptor.model.ffmpeg

@JvmInline
value class FilterId(private val value: String) {
    override fun toString(): String = value
}
