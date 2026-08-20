package dev.scriptor.model.user

import dev.scriptor.instant
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SessionTable : UuidTable("session") {
    val user = reference("user_id", UserTable, ReferenceOption.CASCADE).nullable()
    val token = text("token")
    val createdAt = instant("created_at")
    val expiresAt = instant("expires_at")
    val agent = text("agent").nullable()
    val access = instant("access").nullable()
}

class Session(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Session>(SessionTable)

    var user by User optionalReferencedOn SessionTable.user
    var token by SessionTable.token
    var createdAt by SessionTable.createdAt
    var expiresAt by SessionTable.expiresAt
    var agent by SessionTable.agent
    var access by SessionTable.access

    override fun toString(): String {
        return "Session(id=$id, user=$user, token=$token, createdAt=$createdAt, expiresAt=$expiresAt, agent=$agent, access=$access)"
    }
}
