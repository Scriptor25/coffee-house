package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("session")
data class Session(

    @Column("id")
    @PrimaryKey
    override var id: Uuid,

    @Column("user_id")
    @ForeignKey("user", "id")
    var userId: Uuid?,

    @Column("token")
    var token: String,

    @Column("created_at")
    var createdAt: Instant,

    @Column("expires_at")
    var expiresAt: Instant,

    @Column("access")
    var access: Instant?,

    @Column("agent")
    var agent: String?,

    @Column("sequence")
    var sequence: Long = 0L,

    @Column("next")
    var next: Long = 0L,
) : Entity
