package dev.scriptor.encoder.subtitle

import dev.scriptor.encoder.Encoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface SubtitleEncoder : Encoder {

    companion object {
        fun find(id: ImplementationId): SubtitleEncoder? = when (id) {
            WebVtt.id -> WebVtt

            else -> null
        }
    }

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
