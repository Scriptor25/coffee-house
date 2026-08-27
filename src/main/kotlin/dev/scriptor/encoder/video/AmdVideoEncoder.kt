package dev.scriptor.encoder.video

import dev.scriptor.Profile
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface AmdVideoEncoder : VideoEncoder {

    companion object {
        fun find(id: ImplementationId): AmdVideoEncoder? = when (id) {
            AV1.id -> AV1
            H264.id -> H264
            HEVC.id -> HEVC

            else -> null
        }
    }

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

        override val id = ImplementationId("av1_amf")
        override val codec = CodecId("av1")

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "high_quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }

    data object H264 : AmdVideoEncoder {

        override val id = ImplementationId("h264_amf")
        override val codec = CodecId("h264")

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }

    data object HEVC : AmdVideoEncoder {

        override val id = ImplementationId("hevc_amf")
        override val codec = CodecId("hevc")

        override fun quality(profile: Profile): String = when (profile) {
            Profile.ARCHIVAL -> "quality"
            Profile.HIGH -> "quality"
            Profile.MEDIUM -> "balanced"
            Profile.LOW -> "speed"
            Profile.POTATO -> "speed"
        }
    }
}
