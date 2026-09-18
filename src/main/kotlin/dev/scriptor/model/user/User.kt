package dev.scriptor.model.user

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
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

@JsonSerializable
class User(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<User>(UserTable)

    @all:JsonProperty("id")
    val jsonId
        get() = id.value

    @JsonProperty
    var name by UserTable.name

    var hash by UserTable.hash

    @JsonProperty
    var role by UserTable.role

    override fun toString(): String {
        return "User(id=$id, name=$name, hash=$hash, role=$role)"
    }
}
