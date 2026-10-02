package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable

@JsonSerializable
data class CreatePlaybackBody(
    @all:JsonProperty
    val name: String,
    @all:JsonProperty
    val items: List<PlaybackItem>,
)
