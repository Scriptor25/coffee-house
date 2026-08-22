package dev.scriptor

import dev.scriptor.backend.VideoBackend
import dev.scriptor.model.ffmpeg.Capabilities

data class Pipeline(
    val capabilities: Capabilities,
    val decode: VideoBackend,
    val split: VideoBackend,
    val scale: VideoBackend,
    val encode: VideoBackend,
) {
    constructor(capabilities: Capabilities, backend: VideoBackend) : this(
        capabilities,
        backend,
        backend,
        backend,
        backend,
    )

    val devices = setOfNotNull(decode.device, split.device, scale.device, encode.device)

    fun split(count: Int): List<String> = buildList {
        this += transition(decode, split)
        this += "split=$count"
    }

    fun encode(): List<String> = transition(split, encode)

    fun scaleEncode(
        width: Int,
        height: Int,
    ): List<String> = transition(split, scale) + scale.scale(width, height) + transition(scale, encode)

    private fun transition(
        src: VideoBackend,
        dst: VideoBackend,
    ): List<String> {
        val sd = src.device
        val dd = dst.device

        return when {
            sd == dd -> emptyList()

            else -> {
                val interop =
                    if (sd == null || dd == null) null
                    else capabilities.getInterop(sd, dd)

                if (interop == null || !interop.derivable) {
                    src.download() + dst.upload()
                } else if (!interop.direct) {
                    listOf("hwmap=derive_device=${dd}")
                } else {
                    listOf("hwmap=derive_device=${dd}:mode=direct")
                }
            }
        }
    }
}
