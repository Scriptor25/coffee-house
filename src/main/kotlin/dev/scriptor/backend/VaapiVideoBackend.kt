package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.VaapiVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object VaapiVideoBackend : VideoBackend {

    override val name = "vaapi"

    override val upload = "hwupload=derive_device=vaapi"
    override val download = "hwdownload"

    override val supportScaleAndFormat = true

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("vaapi backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int, format: String?): String = filter(
        "scale_vaapi",
        "w" to width,
        "h" to height,
        "format" to format,
    )

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> VaapiVideoEncoder.AV1
        VideoCodec.VP8 -> VaapiVideoEncoder.VP8
        VideoCodec.VP9 -> VaapiVideoEncoder.VP9
        VideoCodec.H264 -> VaapiVideoEncoder.H264
        VideoCodec.HEVC -> VaapiVideoEncoder.HEVC
    }
}
