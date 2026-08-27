package dev.scriptor.model.ffmpeg

data class FilterCapabilities(
    val id: FilterId,
    val transform: String,

    val timelineSupport: Boolean,
    val sliceThreading: Boolean,
)
