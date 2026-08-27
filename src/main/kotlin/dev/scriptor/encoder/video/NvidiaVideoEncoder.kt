package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface NvidiaVideoEncoder : VideoEncoder {

    companion object {
        fun find(id: ImplementationId): NvidiaVideoEncoder? = when (id) {
            AV1.id -> AV1
            H264.id -> H264
            HEVC.id -> HEVC

            else -> null
        }
    }

    fun preset(profile: Profile): String = when (profile) {
        Profile.ARCHIVAL -> "p1"
        Profile.HIGH -> "p3"
        Profile.MEDIUM -> "p5"
        Profile.LOW -> "p6"
        Profile.POTATO -> "p7"
    }

    data object AV1 : NvidiaVideoEncoder {

        override val id = ImplementationId("av1_nvenc")
        override val codec = CodecId("av1")

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

        override val id = ImplementationId("h264_nvenc")
        override val codec = CodecId("h264")

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

        override val id = ImplementationId("hevc_nvenc")
        override val codec = CodecId("hevc")

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
