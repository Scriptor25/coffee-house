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

    operator fun invoke(index: Int): List<String>

    data object Null : SubtitleEncoder {

        override val id = ImplementationId("null")
        override val codec = CodecId("null")

        override fun invoke(index: Int): List<String> = error("null")
    }

    data class Generic(
        override val id: ImplementationId,
        override val codec: CodecId,
    ) : SubtitleEncoder {

        override fun invoke(index: Int): List<String> = emptyList()
    }

    data object WebVtt : SubtitleEncoder {

        override val id = ImplementationId("webvtt")
        override val codec = CodecId("webvtt")

        override fun invoke(index: Int): List<String> = emptyList()
    }
}
