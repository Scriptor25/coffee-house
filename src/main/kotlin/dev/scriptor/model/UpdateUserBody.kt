package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable

@JsonSerializable
data class UpdateUserBody(
    @all:JsonProperty
    val username: String,
    @all:JsonProperty
    val role: String,
)
