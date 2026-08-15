package dev.scriptor

import dev.scriptor.backend.SoftwareVideoBackend
import dev.scriptor.backend.VideoBackend

data class Pipeline(
    val bitDepth: Int,
    val decode: VideoBackend,
    val split: VideoBackend,
    val scale: VideoBackend,
    val encode: VideoBackend,
) {

    val devices: Set<String>
        get() = buildSet {
            if (decode !is SoftwareVideoBackend) {
                this += decode.name
            }
            if (split !is SoftwareVideoBackend) {
                this += split.name
            }
            if (scale !is SoftwareVideoBackend) {
                this += scale.name
            }
            if (encode !is SoftwareVideoBackend) {
                this += encode.name
            }
        }

    fun buildSplit(count: Int): String =
        buildList<String> {
            this += transition(decode, split)
            this += "split=$count"
        }
            .joinToString(",")

    fun buildEncode(): String =
        transition(split, encode)
            .ifEmpty { listOf("null") }
            .joinToString(",")

    fun buildScaleEncode(
        width: Int,
        height: Int,
    ): String =
        buildList<String> {
            this += transition(split, scale)
            this += scale.scale(width, height)
            this += transition(scale, encode)
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
