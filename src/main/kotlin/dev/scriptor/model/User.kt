package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("user")
data class User(

    @Column(type = String::class)
    @PrimaryKey
    val id: Uuid,

    @Column
    val name: String,

    @Column
    val hash: String,

    @Column(type = String::class)
    val role: UserRole,
)
