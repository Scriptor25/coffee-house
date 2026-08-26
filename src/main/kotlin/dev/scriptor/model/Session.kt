package dev.scriptor.model

import dev.scriptor.model.user.UserRole
import dev.scriptor.security.Jwt
import kotlin.uuid.Uuid

data class Session(
    val jwt: Jwt,
    val id: Uuid?,
    val role: UserRole,
)
