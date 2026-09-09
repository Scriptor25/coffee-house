package dev.scriptor.model

import dev.scriptor.model.user.UserRole

data class CreateUserBody(
    val username: String,
    val password: String,
    val role: UserRole,
)
