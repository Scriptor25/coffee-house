package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.user.User
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class UserJsonConverter : Converter<User, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: User): JsonNode = db { value.toJson() }
}
