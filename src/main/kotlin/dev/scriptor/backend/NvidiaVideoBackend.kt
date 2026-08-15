package dev.scriptor.backend

import dev.scriptor.VideoCodec
import dev.scriptor.encoder.video.NvidiaVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object NvidiaVideoBackend : VideoBackend {

    override val name = "cuda"

    override val upload = "hwupload_cuda"
    override val download = "hwdownload"

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("nvidia backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int): String = "scale_cuda=$width:$height"

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> NvidiaVideoEncoder.AV1
        VideoCodec.H264 -> NvidiaVideoEncoder.H264
        VideoCodec.H265 -> NvidiaVideoEncoder.H265

        else -> error("nvidia backend does not support codec $codec")
    }
}
