package dev.scriptor.model

import java.nio.file.Path
import java.sql.ResultSet
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

data class Media(
    override val id: Uuid,

    val path: Path,
    val modified: Instant,
) : Entity
