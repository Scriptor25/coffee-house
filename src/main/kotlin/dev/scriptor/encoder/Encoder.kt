package dev.scriptor.encoder

import dev.scriptor.model.ffmpeg.ImplementationId

interface Encoder {

    val id: ImplementationId
}
