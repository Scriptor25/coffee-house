package dev.scriptor

enum class Codec(val value: String) {
    COPY("copy"),
    AV1("libsvtav1"),
    VP8("libvpx-vp8"),
    VP9("libvpx-vp9"),
    H264("libx264"),
    H265("libx265"),

    AAC("aac"),
    OPUS("libopus"),
    MP3("libmp3lame"),
    VORBIS("libvorbis"),
    FLAC("flac"),
}
