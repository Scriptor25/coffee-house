package dev.scriptor.encoder.audio

import dev.scriptor.encoder.Encoder
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.ffmpeg.ImplementationId

interface AudioEncoder : Encoder {

    val codec: CodecId

    operator fun invoke(
        index: Int,
    ): List<String>

    data object Aac : AudioEncoder {

        override val id = ImplementationId("aac")
        override val codec = CodecId("aac")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Opus : AudioEncoder {

        override val id = ImplementationId("libopus")
        override val codec = CodecId("opus")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Mp3 : AudioEncoder {

        override val id = ImplementationId("libmp3lame")
        override val codec = CodecId("mp3")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Vorbis : AudioEncoder {

        override val id = ImplementationId("libvorbis")
        override val codec = CodecId("vorbis")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }

    data object Flac : AudioEncoder {

        override val id = ImplementationId("flac")
        override val codec = CodecId("flac")

        override fun invoke(
            index: Int,
        ): List<String> = emptyList()
    }
}
