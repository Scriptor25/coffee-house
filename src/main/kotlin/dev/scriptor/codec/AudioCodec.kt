package dev.scriptor.codec

import dev.scriptor.encoder.audio.AudioEncoder

enum class AudioCodec(val encoder: AudioEncoder) {
    AAC(AudioEncoder.Aac),
    OPUS(AudioEncoder.Opus),
    MP3(AudioEncoder.Mp3),
    VORBIS(AudioEncoder.Vorbis),
    FLAC(AudioEncoder.Flac),
}
