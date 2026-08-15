package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.backend.VulkanVideoBackend
import dev.scriptor.codec.VideoCodec

interface VulkanVideoEncoder : VideoEncoder {

    data object AV1 : IntelVideoEncoder {

        override val name = "av1_vulkan"

        override val backend = VulkanVideoBackend
        override val codec = VideoCodec.AV1

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-qp:v:$index", qp(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun qp(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 25
            Profile.MEDIUM -> 30
            Profile.LOW -> 35
            Profile.POTATO -> 40
        }
    }

    data object H264 : IntelVideoEncoder {

        override val name = "h264_vulkan"

        override val backend = VulkanVideoBackend
        override val codec = VideoCodec.H264

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-qp:v:$index", qp(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun qp(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 20
            Profile.MEDIUM -> 22
            Profile.LOW -> 24
            Profile.POTATO -> 26
        }
    }

    data object HEVC : IntelVideoEncoder {

        override val name = "hevc_vulkan"

        override val backend = VulkanVideoBackend
        override val codec = VideoCodec.HEVC

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-qp:v:$index", qp(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun qp(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 24
            Profile.HIGH -> 26
            Profile.MEDIUM -> 28
            Profile.LOW -> 30
            Profile.POTATO -> 32
        }
    }
}
