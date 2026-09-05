package dev.scriptor.model

data class OffsetLimit(
    val offset: Long = 0L,
    val limit: Int = Int.MAX_VALUE,
)
