package dev.scriptor

import dev.scriptor.backend.VideoBackend
import dev.scriptor.model.ffmpeg.Capabilities

data class Pipeline(
    val decode: VideoBackend,
    val split: VideoBackend,
    val scale: VideoBackend,
    val encode: VideoBackend,
) {
    constructor(backend: VideoBackend) : this(
        backend,
        backend,
        backend,
        backend,
    )

    val devices = setOfNotNull(decode.device, split.device, scale.device, encode.device)

    fun filter(name: String, vararg args: Pair<String?, Any?>): String {
        val args = args
            .filter { it.second != null }
            .joinToString(":") { (k, v) -> if (k == null) "$v" else "$k=$v" }
        return "$name=$args"
    }

    fun split(count: Int): List<String> =
        transition(decode, split) + filter("split", null to count)

    fun encode(): List<String> =
        transition(split, encode)

    fun scaleEncode(
        width: Int,
        height: Int,
    ): List<String> =
        transition(split, scale) + scale.scale(width, height) + transition(scale, encode)

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
                    else Capabilities.getDeviceToDevice(sd, dd)

                if (interop == null || !interop.derivable) {
                    src.download() + dst.upload()
                } else if (!interop.direct) {
                    listOf(filter("hwmap", "derive_device" to dd))
                } else {
                    listOf(filter("hwmap", "derive_device" to dd, "mode" to "direct"))
                }
            }
        }
    }
}
