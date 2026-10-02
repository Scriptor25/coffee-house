package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@JsonSerializable
data class PlaybackItem(
    val id: Uuid,
    val title: String?,
)

@JsonSerializable
data class Playback(
    @all:JsonProperty
    val userId: Uuid,
    @all:JsonProperty
    val name: String,
    @all:JsonProperty
    val items: List<PlaybackItem>,
    @all:JsonProperty
    val createdAt: Instant,
    @all:JsonProperty
    val expiresAt: Instant,
)
