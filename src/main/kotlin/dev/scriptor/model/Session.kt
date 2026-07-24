package dev.scriptor.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

class Session(
    val id: Uuid,
    var access: Instant,
) {

    var open: Boolean = true

    var sequence: Long = 0L
    var next: Long = 0L
}