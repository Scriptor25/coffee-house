package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.AmdVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object AmdVideoBackend : VideoBackend {

    override val name = "amf"

    override val upload = "hwupload"
    override val download = "hwdownload"

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("amd backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "vpp_amf=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> AmdVideoEncoder.AV1
        VideoCodec.H264 -> AmdVideoEncoder.H264
        VideoCodec.HEVC -> AmdVideoEncoder.HEVC

        else -> error("amd backend does not support codec $codec")
    }
}
