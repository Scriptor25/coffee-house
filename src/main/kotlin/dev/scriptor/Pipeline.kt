package dev.scriptor

import dev.scriptor.backend.VideoBackend

data class Pipeline(
    val bitDepth: Int,
    val decode: VideoBackend,
    val scale: VideoBackend,
    val encode: VideoBackend,
) {

    fun build(): String =
        transition(decode, encode)
            .ifEmpty { listOf("null") }
            .joinToString(",")

    fun build(
        width: Int,
        height: Int,
    ): String =
        buildList {
            addAll(transition(decode, scale))
            add(scale.scale(width, height))
            addAll(transition(scale, encode))
        }
            .ifEmpty { listOf("null") }
            .joinToString(",")

    private fun transition(
        src: VideoBackend,
        dst: VideoBackend,
    ): List<String> = when (src) {
        dst -> emptyList()

        else -> listOfNotNull(
            src.download,
            "format=${dst.format(bitDepth)}",
            dst.upload,
        )
    }
}
