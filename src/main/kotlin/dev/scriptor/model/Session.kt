package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("session")
data class Session(

    @Column(type = String::class)
    @PrimaryKey
    val id: Uuid,

    @Column("user_id", String::class)
    @ForeignKey("user", "id")
    val userId: Uuid?,

    @Column
    val token: String,

    @Column("created_at", type = String::class)
    val createdAt: Instant,

    @Column("expires_at", type = String::class)
    val expiresAt: Instant,

    @Column
    val agent: String?,

    @Column(type = String::class)
    var access: Instant? = null,

    @Column
    var sequence: Long = 0L,

    @Column
    var next: Long = 0L,
)
