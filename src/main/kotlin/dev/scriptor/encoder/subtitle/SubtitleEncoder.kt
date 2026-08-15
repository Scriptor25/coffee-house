package dev.scriptor.encoder.subtitle

import dev.scriptor.encoder.Encoder

interface SubtitleEncoder : Encoder {

    operator fun invoke(
        index: Int,
    ): List<String>

    data object WebVtt : SubtitleEncoder {

        override val name = "webvtt"

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }
}
