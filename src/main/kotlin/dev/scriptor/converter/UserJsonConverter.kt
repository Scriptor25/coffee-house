package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.user.User
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class UserJsonConverter : Converter<User, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: User): JsonObjectNode {
        return jsonOf(
            "id" to jsonOf(value.id.toString()),
            "name" to jsonOf(value.name),
            "role" to jsonOf(value.role.name.lowercase()),
        )
    }
}
