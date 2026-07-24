package dev.scriptor.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

class Session(val id: Uuid) {
    lateinit var access: Instant

    var sequence: Long = 0L
    var next: Long = 0L
}