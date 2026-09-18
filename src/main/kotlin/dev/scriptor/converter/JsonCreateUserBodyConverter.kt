package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.model.CreateUserBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonCreateUserBodyConverter : Converter<JsonNode, CreateUserBody> {

    context(provider: Provider?)
    override fun convert(value: JsonNode): CreateUserBody = value.fromJson()
}
