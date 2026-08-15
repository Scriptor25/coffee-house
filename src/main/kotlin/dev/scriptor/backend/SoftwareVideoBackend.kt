package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.VideoEncoder

data object SoftwareVideoBackend : VideoBackend {

    override val name = "none"

    override val upload = null
    override val download = null

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "yuv420p"
        10 -> "yuv420p10le"
        12 -> "yuv420p12le"

        else -> error("software backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "scale=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> VideoEncoder.AV1
        VideoCodec.VP8 -> VideoEncoder.VP8
        VideoCodec.VP9 -> VideoEncoder.VP9
        VideoCodec.H264 -> VideoEncoder.H264
        VideoCodec.H265 -> VideoEncoder.H265
    }
}
