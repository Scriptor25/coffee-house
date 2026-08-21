package dev.scriptor.backend

import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.ffmpeg.DeviceId

data class VideoBackend(
    val device: DeviceId?,
    val upload: () -> List<String>,
    val download: () -> List<String>,
    val decode: () -> List<String>,
    val scale: (width: Int, height: Int) -> List<String>,
    val encoder: VideoEncoder,
)
