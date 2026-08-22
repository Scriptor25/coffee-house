package dev.scriptor.decoder.video

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface VideoDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): VideoDecoder? = when (id) {
            else -> null
        }
    }

    val codec: CodecId

    operator fun invoke(index: Int): List<String>

    data object Null : VideoDecoder {

        override val id = ImplementationId("null")
        override val codec = CodecId("null")

        override fun invoke(index: Int): List<String> = error("null")
    }

    data class Generic(
        override val id: ImplementationId,
        override val codec: CodecId,
    ) : VideoDecoder {

        override fun invoke(index: Int): List<String> = emptyList()
    }
}
