package dev.scriptor.decoder.subtitle

import dev.scriptor.decoder.Decoder
import dev.scriptor.model.ffmpeg.ImplementationId

sealed interface SubtitleDecoder : Decoder {

    companion object {
        fun find(id: ImplementationId): SubtitleDecoder? = when (id) {
            else -> null
        }
    }

    operator fun invoke(index: Int): List<String>
}
