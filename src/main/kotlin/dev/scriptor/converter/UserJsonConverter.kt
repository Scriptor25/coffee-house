package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.user.User
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class UserJsonConverter : Converter<User, JsonNode> {

    context(provider: Provider)
    override fun convert(value: User): JsonNode {
        val database = provider.getContextT<Database>()
            ?: error("missing database context")

        lateinit var sessions: JsonNode

        transaction(database) {
            sessions = provider(value.sessions.toList())
        }

        return jsonOf(
            "id" to jsonOf(value.id),
            "name" to jsonOf(value.name),
            "role" to jsonOf(value.role),
            "sessions" to sessions,
        )
    }
}
