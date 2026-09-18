package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.fromJson
import dev.scriptor.model.UpdateUserBody
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JsonUpdateUserBodyConverter : Converter<JsonNode, UpdateUserBody> {

    context(provider: Provider?)
    override fun convert(value: JsonNode): UpdateUserBody = value.fromJson()
}
