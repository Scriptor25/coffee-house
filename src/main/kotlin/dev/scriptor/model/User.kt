package dev.scriptor.model

import kotlin.uuid.Uuid

data class User(
    override val id: Uuid,

    val name: String,
) : Entity
