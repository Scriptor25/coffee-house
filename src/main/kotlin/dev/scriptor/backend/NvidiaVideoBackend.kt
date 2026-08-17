package dev.scriptor.backend

import dev.scriptor.codec.VideoCodec
import dev.scriptor.encoder.video.NvidiaVideoEncoder
import dev.scriptor.encoder.video.VideoEncoder

data object NvidiaVideoBackend : VideoBackend {

    override val name = "cuda"

    override val upload = "hwupload_cuda"
    override val download = "hwdownload"

    override val supportScaleAndFormat = true

    override fun format(bitDepth: Int): String = when (bitDepth) {
        8 -> "nv12"
        10 -> "p010le"

        else -> error("nvidia backend does not support bit depth $bitDepth")
    }

    override fun scale(width: Int, height: Int, format: String?): String = filter(
        "scale_cuda",
        "w" to width,
        "h" to height,
        "format" to format,
    )

    override fun encoder(codec: VideoCodec): VideoEncoder = when (codec) {
        VideoCodec.AV1 -> NvidiaVideoEncoder.AV1
        VideoCodec.H264 -> NvidiaVideoEncoder.H264
        VideoCodec.HEVC -> NvidiaVideoEncoder.HEVC

        else -> error("nvidia backend does not support codec $codec")
    }
}
