package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.VideoEncoder

data object VulkanVideoBackend : VideoBackend {

    override val name = "vulkan"

    override val upload = "hwupload"
    override val download = "hwdownload"

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("vulkan backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "scale_vulkan=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> TODO()
        VideoCodec.H264 -> TODO()
        VideoCodec.HEVC -> TODO()

        else -> error("vulkan backend does not support codec $codec")
    }
}
