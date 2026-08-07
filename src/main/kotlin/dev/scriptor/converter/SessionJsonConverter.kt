package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Session
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class SessionJsonConverter : Converter<Session, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Session): JsonNode {
        return jsonOf(
            "id" to jsonOf(value.id),
            "user_id" to jsonOf(value.userId),
            "token" to jsonOf(value.token),
            "created_at" to jsonOf(value.createdAt),
            "expires_at" to jsonOf(value.expiresAt),
            "access" to jsonOf(value.access),
            "agent" to jsonOf(value.agent),
            "sequence" to jsonOf(value.sequence),
            "next" to jsonOf(value.next),
        )
    }
}
