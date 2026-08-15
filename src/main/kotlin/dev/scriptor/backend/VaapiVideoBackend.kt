package dev.scriptor.backend

import dev.scriptor.VideoCodec
import dev.scriptor.encoder.video.VaapiVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object VaapiVideoBackend : VideoBackend {

    override val name = "vaapi"

    override val upload = "hwupload"
    override val download = "hwdownload"

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("vaapi backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "scale_vaapi=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> VaapiVideoEncoder.AV1
        VideoCodec.H264 -> VaapiVideoEncoder.H264
        VideoCodec.H265 -> VaapiVideoEncoder.H265

        else -> error("vaapi backend does not support codec $codec")
    }
}
