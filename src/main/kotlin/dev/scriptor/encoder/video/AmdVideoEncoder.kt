package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.VideoCodec
import dev.scriptor.backend.AmdVideoBackend

interface AmdVideoEncoder : VideoEncoder {

    override fun invoke(
        index: Int,
        profile: Profile,
        bitrate: Long,
    ): List<String> = listOf(
        "-quality:v:$index", quality(profile),
        "-rc:v:$index", "vbr_peak",
        "-b:v:$index", bitrate.toString(),
        "-maxrate:v:$index", bitrate.toString(),
        "-bufsize:v:$index", (bitrate * 2).toString(),
    )

    fun quality(profile: Profile): String

    data object AV1 : AmdVideoEncoder {

        override val name = "av1_amf"

        override val backend = AmdVideoBackend
        override val codec = VideoCodec.AV1

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "high_quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }

    data object H264 : AmdVideoEncoder {

        override val name = "h264_amf"

        override val backend = AmdVideoBackend
        override val codec = VideoCodec.H264

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }

    data object H265 : AmdVideoEncoder {

        override val name = "hevc_amf"

        override val backend = AmdVideoBackend
        override val codec = VideoCodec.H265

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }
}
