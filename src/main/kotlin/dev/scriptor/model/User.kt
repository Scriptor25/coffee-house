package dev.scriptor.model

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object UserTable : UuidTable("user") {
    val name = text("name")
    val hash = text("hash")
    val role = enumeration("role", UserRole::class)
}

class User(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<User>(UserTable)

    var name by UserTable.name
    var hash by UserTable.hash
    var role by UserTable.role

    val sessions by Session optionalReferrersOn SessionTable.user

    override fun toString(): String {
        return "User(id=$id, name=$name, hash=$hash, role=$role)"
    }
}
