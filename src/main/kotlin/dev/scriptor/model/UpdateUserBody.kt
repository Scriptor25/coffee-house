package dev.scriptor.model

import dev.scriptor.model.user.UserRole

data class UpdateUserBody(
    val username: String,
    val role: UserRole,
)
