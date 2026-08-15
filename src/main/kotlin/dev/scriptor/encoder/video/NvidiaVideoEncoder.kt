package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.backend.NvidiaVideoBackend
import dev.scriptor.codec.VideoCodec

interface NvidiaVideoEncoder : VideoEncoder {

    fun preset(profile: Profile): String = when (profile) {
        Profile.ARCHIVAL -> "p1"
        Profile.HIGH -> "p3"
        Profile.MEDIUM -> "p5"
        Profile.LOW -> "p6"
        Profile.POTATO -> "p7"
    }

    data object AV1 : NvidiaVideoEncoder {

        override val name = "av1_nvenc"

        override val backend = NvidiaVideoBackend
        override val codec = VideoCodec.AV1

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-rc:v:$index", "vbr",
            "-cq:v:$index", cq(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun cq(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 25
            Profile.MEDIUM -> 30
            Profile.LOW -> 35
            Profile.POTATO -> 40
        }
    }

    data object H264 : NvidiaVideoEncoder {

        override val name = "h264_nvenc"

        override val backend = NvidiaVideoBackend
        override val codec = VideoCodec.H264

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-rc:v:$index", "vbr",
            "-cq:v:$index", cq(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun cq(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 20
            Profile.MEDIUM -> 22
            Profile.LOW -> 24
            Profile.POTATO -> 26
        }
    }

    data object HEVC : NvidiaVideoEncoder {

        override val name = "hevc_nvenc"

        override val backend = NvidiaVideoBackend
        override val codec = VideoCodec.HEVC

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-rc:v:$index", "vbr",
            "-cq:v:$index", cq(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun cq(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 22
            Profile.HIGH -> 24
            Profile.MEDIUM -> 26
            Profile.LOW -> 28
            Profile.POTATO -> 30
        }
    }
}
