package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.encoder.Encoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

interface VideoEncoder : Encoder {

    companion object {
        fun find(id: ImplementationId): VideoEncoder? = when (id) {
            AV1.id -> AV1
            VP8.id -> VP8
            VP9.id -> VP9
            H264.id -> H264
            HEVC.id -> HEVC

            else -> AmdVideoEncoder.find(id)
                ?: IntelVideoEncoder.find(id)
                ?: NvidiaVideoEncoder.find(id)
                ?: VaapiVideoEncoder.find(id)
                ?: VulkanVideoEncoder.find(id)
        }
    }

    val codec: CodecId

    operator fun invoke(
        index: Int,
        profile: Profile,
        bitrate: Long,
    ): List<String>

    data object AV1 : VideoEncoder {

        override val id = ImplementationId("libsvtav1")
        override val codec = CodecId("av1")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun preset(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 3
            Profile.HIGH -> 5
            Profile.MEDIUM -> 7
            Profile.LOW -> 9
            Profile.POTATO -> 11
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 25
            Profile.MEDIUM -> 30
            Profile.LOW -> 35
            Profile.POTATO -> 40
        }
    }

    data object VP8 : VideoEncoder {

        override val id = ImplementationId("libvpx-vp8")
        override val codec = CodecId("vp8")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-cpu-used:v:$index", cpuUsed(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun cpuUsed(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 0
            Profile.HIGH -> 1
            Profile.MEDIUM -> 2
            Profile.LOW -> 4
            Profile.POTATO -> 6
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 24
            Profile.MEDIUM -> 30
            Profile.LOW -> 36
            Profile.POTATO -> 42
        }
    }

    data object VP9 : VideoEncoder {

        override val id = ImplementationId("libvpx-vp9")
        override val codec = CodecId("vp9")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-cpu-used:v:$index", cpuUsed(profile).toString(),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun cpuUsed(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 0
            Profile.HIGH -> 1
            Profile.MEDIUM -> 2
            Profile.LOW -> 4
            Profile.POTATO -> 6
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 24
            Profile.MEDIUM -> 30
            Profile.LOW -> 36
            Profile.POTATO -> 42
        }
    }

    data object H264 : VideoEncoder {

        override val id = ImplementationId("libx264")
        override val codec = CodecId("h264")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun preset(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "veryslow"
            Profile.HIGH -> "slow"
            Profile.MEDIUM -> "medium"
            Profile.LOW -> "veryfast"
            Profile.POTATO -> "superfast"
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 20
            Profile.MEDIUM -> 22
            Profile.LOW -> 24
            Profile.POTATO -> 26
        }
    }

    data object HEVC : VideoEncoder {

        override val id = ImplementationId("libx265")
        override val codec = CodecId("hevc")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-preset:v:$index", preset(profile),
            "-crf:v:$index", crf(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun preset(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "veryslow"
            Profile.HIGH -> "slow"
            Profile.MEDIUM -> "medium"
            Profile.LOW -> "veryfast"
            Profile.POTATO -> "superfast"
        }

        private fun crf(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 24
            Profile.HIGH -> 26
            Profile.MEDIUM -> 28
            Profile.LOW -> 30
            Profile.POTATO -> 32
        }
    }
}
