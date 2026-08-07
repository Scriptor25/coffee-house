package dev.scriptor.model

import dev.scriptor.instant
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

object SessionTable : UuidTable("session") {
    val user = reference("user_id", UserTable).nullable()
    val token = text("token")
    val createdAt = instant("created_at")
    val expiresAt = instant("expires_at")
    val agent = text("agent").nullable().default(null)
    val access = instant("access").nullable().default(null)
    val sequence = long("sequence").default(0L)
    val next = long("next").default(0L)
}

class Session(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<Session>(SessionTable)

    var user by SessionTable.user
    var token by SessionTable.token
    var createdAt by SessionTable.createdAt
    var expiresAt by SessionTable.expiresAt
    var agent by SessionTable.agent
    var access by SessionTable.access
    var sequence by SessionTable.sequence
    var next by SessionTable.next

    override fun toString(): String {
        return "Session(id=$id, user=$user, token=$token, createdAt=$createdAt, expiresAt=$expiresAt, agent=$agent, access=$access, sequence=$sequence, next=$next)"
    }
}
