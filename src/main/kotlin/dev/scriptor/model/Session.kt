package dev.scriptor.model

import dev.scriptor.Entity
import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("session")
data class Session(

    @Column
    override val id: Uuid,

    @Column("user_id")
    val user: User?,

    @Column(unique = "cnt_token")
    val token: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("expires_at")
    var expiresAt: Instant,

    @Column
    val agent: String?,

    @Column
    var access: Instant? = null,

    @Column
    var sequence: Long = 0L,

    @Column
    var next: Long = 0L,
) : Entity
