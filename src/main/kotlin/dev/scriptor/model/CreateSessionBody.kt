package dev.scriptor.model

import dev.scriptor.model.user.UserRole

data class CreateSessionBody(
    val username: String,
    val password: String,
)
