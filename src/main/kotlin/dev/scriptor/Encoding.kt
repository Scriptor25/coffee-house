package dev.scriptor

import dev.scriptor.encoder.audio.AudioEncoder
import dev.scriptor.encoder.subtitle.SubtitleEncoder
import dev.scriptor.encoder.video.VideoEncoder

sealed interface VideoEncoding {

    operator fun invoke(index: Int, encoder: VideoEncoder): List<String>

    data object Copy : VideoEncoding {

        override fun invoke(index: Int, encoder: VideoEncoder): List<String> =
            listOf("-c:v:$index", "copy")
    }

    data class Transcode(
        val profile: Profile,
        val bitrate: Long,
    ) : VideoEncoding {

        override fun invoke(index: Int, encoder: VideoEncoder): List<String> =
            listOf("-c:v:$index", "${encoder.id}") + encoder(index, profile, bitrate)
    }
}

sealed interface AudioEncoding {

    operator fun invoke(index: Int, encoder: AudioEncoder): List<String>

    data object Copy : AudioEncoding {

        override fun invoke(index: Int, encoder: AudioEncoder): List<String> =
            listOf("-c:a:$index", "copy")
    }

    data object Transcode : AudioEncoding {

        override fun invoke(index: Int, encoder: AudioEncoder): List<String> =
            listOf("-c:a:$index", "${encoder.id}") + encoder(index)
    }
}

sealed interface SubtitleEncoding {

    operator fun invoke(index: Int, encoder: SubtitleEncoder): List<String>

    data object Copy : SubtitleEncoding {

        override fun invoke(index: Int, encoder: SubtitleEncoder): List<String> =
            listOf("-c:s:$index", "copy")
    }

    data object Transcode : SubtitleEncoding {

        override fun invoke(index: Int, encoder: SubtitleEncoder): List<String> =
            listOf("-c:s:$index", "${encoder.id}") + encoder(index)
    }
}
