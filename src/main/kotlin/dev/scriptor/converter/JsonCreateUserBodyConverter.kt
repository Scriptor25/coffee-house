package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.CreateUserBody
import dev.scriptor.model.user.UserRole
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.reflect.typeOf

class JsonCreateUserBodyConverter : Converter<JsonNode, CreateUserBody> {

    context(provider: Provider)
    override fun convert(value: JsonNode): CreateUserBody = value.cast(
        mapOf(
            typeOf<UserRole>() to { UserRole.valueOf(it.cast<String>().uppercase()) },
        ),
    )
}
