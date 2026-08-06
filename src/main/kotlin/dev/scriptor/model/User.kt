package dev.scriptor.model

import dev.scriptor.Entity
import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("user")
data class User(

    @Column
    override val id: Uuid,

    @Column
    val name: String,

    @Column
    val hash: String,

    @Column
    val role: UserRole,
) : Entity
