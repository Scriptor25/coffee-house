package dev.scriptor.decoder.audio

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface AudioDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): AudioDecoder? = when (id) {
            else -> null
        }
    }

    operator fun invoke(index: Int): List<String>

    data object Null : AudioDecoder {

        override val id = ImplementationId("null")
        override val codec = CodecId("null")

        override fun invoke(index: Int): List<String> = error("null")
    }

    data class Generic(
        override val id: ImplementationId,
        override val codec: CodecId,
    ) : AudioDecoder {

        override fun invoke(index: Int): List<String> = emptyList()
    }

}
