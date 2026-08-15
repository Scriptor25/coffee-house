package dev.scriptor.encoder.audio

import dev.scriptor.AudioCodec
import dev.scriptor.encoder.Encoder

interface AudioEncoder : Encoder {

    val codec: AudioCodec

    operator fun invoke(
        index: Int,
    ): List<String>

    data object Aac : AudioEncoder {

        override val name = "aac"

        override val codec = AudioCodec.AAC

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Opus : AudioEncoder {

        override val name = "libopus"

        override val codec = AudioCodec.OPUS

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Mp3 : AudioEncoder {

        override val name = "libmp3lame"

        override val codec = AudioCodec.MP3

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Vorbis : AudioEncoder {

        override val name = "libvorbis"

        override val codec = AudioCodec.VORBIS

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Flac : AudioEncoder {

        override val name = "flac"

        override val codec = AudioCodec.FLAC

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }
}
