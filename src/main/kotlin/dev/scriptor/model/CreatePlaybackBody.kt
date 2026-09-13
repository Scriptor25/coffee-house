package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import kotlin.uuid.Uuid

@JsonSerializable
data class CreatePlaybackBody(
    @all:JsonProperty
    val name: String,
    @all:JsonProperty
    val items: List<Uuid>,
)
