package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.cast
import dev.scriptor.model.UpdateUserBody
import dev.scriptor.model.user.UserRole
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import kotlin.reflect.typeOf

class JsonUpdateUserBodyConverter : Converter<JsonNode, UpdateUserBody> {

    context(provider: Provider)
    override fun convert(value: JsonNode): UpdateUserBody = value.cast(
        mapOf(
            typeOf<UserRole>() to { UserRole.valueOf(it.cast<String>().uppercase()) },
        ),
    )
}
