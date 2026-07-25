package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("user")
data class User(

    @Column("id")
    override val id: Uuid,

    @Column("name")
    val name: String,

    @Column("hash")
    val hash: String,

    @Column("role")
    val role: UserRole,
) : Entity
