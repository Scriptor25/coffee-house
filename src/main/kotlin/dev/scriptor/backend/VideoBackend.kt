package dev.scriptor.backend

import dev.scriptor.VideoCodec
import dev.scriptor.encoder.video.VideoEncoder

interface VideoBackend {

    val name: String

    val upload: String?
    val download: String?

    fun format(bitDepth: Int): String
    fun scale(width: Int, height: Int): String

    fun encoder(codec: VideoCodec): VideoEncoder
}