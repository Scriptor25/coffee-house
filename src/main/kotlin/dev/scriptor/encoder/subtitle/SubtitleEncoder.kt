package dev.scriptor.encoder.subtitle

import dev.scriptor.codec.SubtitleCodec
import dev.scriptor.encoder.Encoder

interface SubtitleEncoder : Encoder {

    val codec: SubtitleCodec

    operator fun invoke(
        index: Int,
    ): List<String>

    data object WebVtt : SubtitleEncoder {

        override val name = "webvtt"

        override val codec = SubtitleCodec.WEBVTT

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }
}
