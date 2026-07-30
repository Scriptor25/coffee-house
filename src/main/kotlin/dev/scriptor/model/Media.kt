package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import dev.scriptor.annotation.Unique
import java.nio.file.Path
import java.sql.Timestamp
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column(type = String::class)
    @PrimaryKey
    val id: Uuid,

    @Column(type = String::class)
    @Unique
    val path: Path,

    @Column
    val size: Long,

    @Column
    val title: String,

    @Column("created_at", Timestamp::class)
    val createdAt: Instant,

    @Column("modified_at", Timestamp::class)
    val modifiedAt: Instant,
)
