package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable

@JsonSerializable
data class CreateUserBody(
    @all:JsonProperty
    val username: String,
    @all:JsonProperty
    val password: String,
    @all:JsonProperty
    val role: String,
)
