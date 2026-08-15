package dev.scriptor.codec

import dev.scriptor.encoder.subtitle.SubtitleEncoder

enum class SubtitleCodec(val encoder: SubtitleEncoder) {
    WEBVTT(SubtitleEncoder.WebVtt),
}
