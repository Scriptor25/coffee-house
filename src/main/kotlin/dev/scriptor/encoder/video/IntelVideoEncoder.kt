package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

interface IntelVideoEncoder : VideoEncoder {

    companion object {
        fun find(id: ImplementationId): IntelVideoEncoder? = when (id) {
            AV1.id -> AV1
            VP9.id -> VP9
            H264.id -> H264
            HEVC.id -> HEVC

            else -> null
        }
    }

    data object AV1 : IntelVideoEncoder {

        override val id = ImplementationId("av1_qsv")
        override val codec = CodecId("av1")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-global_quality:v:$index", quality(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun quality(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 25
            Profile.MEDIUM -> 30
            Profile.LOW -> 35
            Profile.POTATO -> 40
        }
    }

    data object VP9 : IntelVideoEncoder {

        override val id = ImplementationId("vp9_qsv")
        override val codec = CodecId("vp9")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long
        ): List<String> = listOf(
            "-global_quality:v:$index", quality(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun quality(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 24
            Profile.MEDIUM -> 30
            Profile.LOW -> 36
            Profile.POTATO -> 42
        }
    }

    data object H264 : IntelVideoEncoder {

        override val id = ImplementationId("h264_qsv")
        override val codec = CodecId("h264")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-global_quality:v:$index", quality(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun quality(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 18
            Profile.HIGH -> 20
            Profile.MEDIUM -> 22
            Profile.LOW -> 24
            Profile.POTATO -> 26
        }
    }

    data object HEVC : IntelVideoEncoder {

        override val id = ImplementationId("hevc_qsv")
        override val codec = CodecId("hevc")

        override fun invoke(
            index: Int,
            profile: Profile,
            bitrate: Long,
        ): List<String> = listOf(
            "-global_quality:v:$index", quality(profile).toString(),
            "-b:v:$index", bitrate.toString(),
            "-maxrate:v:$index", bitrate.toString(),
            "-bufsize:v:$index", (bitrate * 2).toString(),
        )

        private fun quality(profile: Profile): Int = when (profile) {
            Profile.ARCHIVAL -> 24
            Profile.HIGH -> 26
            Profile.MEDIUM -> 28
            Profile.LOW -> 30
            Profile.POTATO -> 32
        }
    }
}
