package dev.scriptor.model

import java.nio.file.Path
import java.sql.ResultSet
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

data class Media(
    val id: Uuid,
    val path: Path,
    val modified: Instant,
)

fun ResultSet.getMedia(): Media = Media(
    Uuid.parseHexDash(getString("id")),
    Path.of(getString("path")),
    getTimestamp("modified").toInstant().toKotlinInstant(),
)
