package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("user")
data class User(

    @Column("id")
    @PrimaryKey
    override var id: Uuid,

    @Column("name")
    var name: String,

    @Column("hash")
    var hash: String,

    @Column("role")
    var role: UserRole,
) : Entity
