package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import dev.scriptor.model.user.UserRole

@JsonSerializable
data class UpdateUserBody(
    @all:JsonProperty
    val username: String,
    @all:JsonProperty
    val role: UserRole,
)
