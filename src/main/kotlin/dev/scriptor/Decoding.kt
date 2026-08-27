package dev.scriptor

import dev.scriptor.decoder.audio.AudioDecoder
import dev.scriptor.decoder.subtitle.SubtitleDecoder
import dev.scriptor.decoder.video.VideoDecoder

data object VideoDecoding {

    operator fun invoke(index: Int, decoder: VideoDecoder): List<String> =
        listOf("-c:v:$index", "${decoder.id}") + decoder(index)
}

data object AudioDecoding {

    operator fun invoke(index: Int, decoder: AudioDecoder): List<String> =
        listOf("-c:a:$index", "${decoder.id}") + decoder(index)
}

data object SubtitleDecoding {

    operator fun invoke(index: Int, decoder: SubtitleDecoder): List<String> =
        listOf("-c:s:$index", "${decoder.id}") + decoder(index)
}
