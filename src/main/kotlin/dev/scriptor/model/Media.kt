package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column("id")
    override val id: Uuid,

    @Column("path")
    val path: Path,

    @Column("title")
    val title: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("modified_at")
    val modifiedAt: Instant,
) : Entity
