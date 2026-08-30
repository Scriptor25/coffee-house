package dev.scriptor.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class Playback(
    val userId: Uuid?,
    val name: String,
    val items: List<Uuid>,
    val createdAt: Instant,
    val expiresAt: Instant,
)
