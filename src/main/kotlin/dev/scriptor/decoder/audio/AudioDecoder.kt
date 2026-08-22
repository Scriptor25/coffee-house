package dev.scriptor.decoder.audio

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface AudioDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): AudioDecoder? = when (id) {
            else -> null
        }
    }

    operator fun invoke(index: Int): List<String>
}
