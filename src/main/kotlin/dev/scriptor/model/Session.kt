package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("session")
data class Session(
    
    @Column("id")
    override val id: Uuid,

    @Column("user_id")
    val userId: Uuid,

    @Column("token")
    val token: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("expires_at")
    val expiresAt: Instant,

    @Column("access")
    var access: Instant?,

    @Column("agent")
    val agent: String?,

    @Column("sequence")
    var sequence: Long = 0L,

    @Column("next")
    var next: Long = 0L,
) : Entity
