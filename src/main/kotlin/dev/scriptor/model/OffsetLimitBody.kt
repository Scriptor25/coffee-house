package dev.scriptor.model

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable

@JsonSerializable
data class OffsetLimitBody(
    @all:JsonProperty
    val offset: Long = 0L,
    @all:JsonProperty
    val limit: Int = Int.MAX_VALUE,
)
