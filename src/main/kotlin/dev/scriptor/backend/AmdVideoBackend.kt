package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.AmdVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object AmdVideoBackend : VideoBackend {

    override val name = "amf"

    override val upload = "hwupload=derive_device=amf"
    override val download = "hwdownload"

    override val supportScaleAndFormat = true

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("amd backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int, format: String?): String = filter(
        "vpp_amf",
        "w" to width,
        "h" to height,
        "format" to format,
    )

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> AmdVideoEncoder.AV1
        VideoCodec.H264 -> AmdVideoEncoder.H264
        VideoCodec.HEVC -> AmdVideoEncoder.HEVC

        else -> error("amd backend does not support codec $codec")
    }
}
