package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.VideoEncoder

interface VideoBackend {

    val name: String

    val upload: String?
    val download: String?

    val supportScaleAndFormat: Boolean

    fun format(bitDepth: Int): String
    fun scale(width: Int, height: Int, format: String?): String

    fun encoder(codec: VideoCodec): VideoEncoder

    fun filter(filter: String, vararg args: Pair<String, Any?>): String {
        val args = args
            .filter { it.second != null }
            .joinToString(":") { (key, value) -> "$key=$value" }
        return "$filter=$args"
    }
}
