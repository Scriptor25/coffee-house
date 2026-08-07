package dev.scriptor.model

import dev.scriptor.Entity
import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column
    override val id: Uuid,

    @Column(unique = "cnt_path")
    val path: Path,

    @Column
    val size: Long,

    @Column
    val title: String?,

    @Column("created_at")
    val createdAt: Instant,

    @Column("modified_at")
    val modifiedAt: Instant,

    @Column
    val duration: Double,
) : Entity
