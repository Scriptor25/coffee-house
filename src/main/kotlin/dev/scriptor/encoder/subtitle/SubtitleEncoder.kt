package dev.scriptor.encoder.subtitle

import dev.scriptor.encoder.Encoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

interface SubtitleEncoder : Encoder {

    val codec: CodecId

    operator fun invoke(
        index: Int,
    ): List<String>

    data object WebVtt : SubtitleEncoder {

        override val id = ImplementationId("webvtt")
        override val codec = CodecId("webvtt")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }
}
