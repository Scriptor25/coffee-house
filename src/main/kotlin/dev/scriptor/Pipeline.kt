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

    fun buildSplit(count: Int): String = buildList<String> {
        this += transition(decode, split, true)
        this += "split=$count"
    }
        .joinToString(",")

    fun buildEncode(): String =
        transition(split, encode, true)
            .ifEmpty { listOf("null") }
            .joinToString(",")

    fun buildScaleEncode(
        width: Int,
        height: Int,
    ): String = buildList<String> {
        this += transition(split, scale, true)

        val scaleAndFormat = scale.supportScaleAndFormat && transitionRequiresFormat(scale, encode)

        if (scaleAndFormat) {
            this += scale.scale(width, height, encode.format(bitDepth))
        } else {
            this += scale.scale(width, height, null)
        }

        this += transition(scale, encode, !scaleAndFormat)
    }
        .ifEmpty { listOf("null") }
        .joinToString(",")

    private fun transitionRequiresFormat(
        src: VideoBackend,
        dst: VideoBackend,
    ): Boolean = when (src) {
        dst -> false

        else -> {
            val srcFormat = src.format(bitDepth)
            val dstFormat = dst.format(bitDepth)

            return srcFormat != dstFormat
        }
    }

    private fun transition(
        src: VideoBackend,
        dst: VideoBackend,
        format: Boolean,
    ): List<String> = when (src) {
        dst -> emptyList()

        else -> {
            val srcFormat = src.format(bitDepth)
            val dstFormat = dst.format(bitDepth)

            listOfNotNull(
                src.download,
                if (srcFormat != dstFormat && format) "format=$dstFormat" else null,
                dst.upload,
            )
        }
    }
}
