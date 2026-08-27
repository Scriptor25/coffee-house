package dev.scriptor.decoder.subtitle

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface SubtitleDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): SubtitleDecoder? = when (id) {
            else -> null
        }
    }

    operator fun invoke(index: Int): List<String>

    data object Null : SubtitleDecoder {

        override val id = ImplementationId("null")
        override val codec = CodecId("null")

        override fun invoke(index: Int): List<String> = error("null")
    }

    data class Generic(
        override val id: ImplementationId,
        override val codec: CodecId,
    ) : SubtitleDecoder {

        override fun invoke(index: Int): List<String> = emptyList()
    }
}
