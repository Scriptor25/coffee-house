package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable

@JsonSerializable
data class CreateSessionBody(
    @all:JsonProperty
    val username: String,
    @all:JsonProperty
    val password: String,
)
