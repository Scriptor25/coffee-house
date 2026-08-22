package dev.scriptor.encoder

import dev.scriptor.encoder.audio.AudioEncoder
import dev.scriptor.encoder.subtitle.SubtitleEncoder
import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.ffmpeg.ImplementationId

interface Encoder {

    companion object {
        fun find(id: ImplementationId): Encoder? =
            VideoEncoder.find(id)
                ?: AudioEncoder.find(id)
                ?: SubtitleEncoder.find(id)
    }

    val id: ImplementationId
}
