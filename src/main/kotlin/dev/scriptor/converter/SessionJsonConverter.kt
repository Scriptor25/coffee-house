package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.user.Session
import dev.scriptor.model.user.User
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SessionJsonConverter : Converter<Session, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Session): JsonNode {
        val database = provider.getContextT<Database>()
            ?: error("missing database context")

        var user: User? = null

        transaction(database) {
            user = value.user
        }

        return jsonOf(
            "user_id" to jsonOf(user?.id),
            "token" to jsonOf(value.token),
            "created_at" to jsonOf(value.createdAt),
            "expires_at" to jsonOf(value.expiresAt),
            "agent" to jsonOf(value.agent),
            "access" to jsonOf(value.access),
        )
    }
}
