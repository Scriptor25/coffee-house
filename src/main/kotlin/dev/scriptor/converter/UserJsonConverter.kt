package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.user.User
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class UserJsonConverter : Converter<User, JsonNode> {

    context(provider: Provider)
    override fun convert(value: User): JsonNode {
        return jsonOf(
            "id" to jsonOf(value.id),
            "name" to jsonOf(value.name),
            "role" to jsonOf(value.role),
        )
    }
}
