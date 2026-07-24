package dev.scriptor.model

import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class MediaModel(
    val id: Uuid,
    val path: Path,
    val modified: Instant,
)
