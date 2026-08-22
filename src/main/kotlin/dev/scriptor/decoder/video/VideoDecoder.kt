package dev.scriptor.decoder.video

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface VideoDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): VideoDecoder? = when (id) {
            else -> null
        }
    }

    operator fun invoke(index: Int): List<String>

    data object Null : VideoDecoder {

        override val id = ImplementationId("null")

        override fun invoke(index: Int): List<String> = error("null")
    }
}
