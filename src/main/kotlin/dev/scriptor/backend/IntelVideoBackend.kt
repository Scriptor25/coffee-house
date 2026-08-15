package dev.scriptor.backend

import dev.scriptor.VideoCodec
import dev.scriptor.encoder.video.IntelVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object IntelVideoBackend : VideoBackend {

    override val name = "qsv"

    override val upload = "hwupload"
    override val download = "hwdownload"

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("intel backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "scale_qsv=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> IntelVideoEncoder.AV1
        VideoCodec.H264 -> IntelVideoEncoder.H264
        VideoCodec.H265 -> IntelVideoEncoder.H265

        else -> error("intel backend does not support codec $codec")
    }
}
