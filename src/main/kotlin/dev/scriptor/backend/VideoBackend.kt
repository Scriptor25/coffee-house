package dev.scriptor.backend

import dev.scriptor.decoder.video.VideoDecoder
import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.ffmpeg.DeviceId

interface VideoBackend {
    val device: DeviceId?

    val decoder: VideoDecoder
    val encoder: VideoEncoder

    fun upload(): List<String>
    fun download(): List<String>

    fun scale(width: Int, height: Int): List<String>
}
