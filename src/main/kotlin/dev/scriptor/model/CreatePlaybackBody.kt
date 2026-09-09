package dev.scriptor.model

import kotlin.uuid.Uuid

data class CreatePlaybackBody(
    val name: String,
    val items: List<Uuid>,
)
