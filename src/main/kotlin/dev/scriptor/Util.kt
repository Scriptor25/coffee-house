package dev.scriptor

import dev.scriptor.model.Media
import java.nio.file.Path
import java.sql.ResultSet
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

fun ResultSet.getMedia(): Media = Media(
    Uuid.parseHexDash(getString("id")),
    Path.of(getString("path")),
    getTimestamp("modified").toInstant().toKotlinInstant(),
)
