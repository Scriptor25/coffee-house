package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import dev.scriptor.annotation.Unique
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column("id")
    @PrimaryKey
    override var id: Uuid,

    @Column("path")
    @Unique
    var path: Path,

    @Column("size")
    var size: Long,

    @Column("title")
    var title: String,

    @Column("created_at")
    var createdAt: Instant,

    @Column("modified_at")
    var modifiedAt: Instant,
) : Entity
