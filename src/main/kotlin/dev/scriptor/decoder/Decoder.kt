package dev.scriptor.decoder

import dev.scriptor.decoder.audio.AudioDecoder
import dev.scriptor.decoder.subtitle.SubtitleDecoder
import dev.scriptor.decoder.video.VideoDecoder
import dev.scriptor.model.ffmpeg.ImplementationId

interface Decoder {

    companion object {
        fun find(id: ImplementationId): Decoder? =
            VideoDecoder.find(id)
                ?: AudioDecoder.find(id)
                ?: SubtitleDecoder.find(id)
    }

    val id: ImplementationId
}
