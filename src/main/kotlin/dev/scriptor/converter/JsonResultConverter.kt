package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.result.StringResult

class JsonResultConverter : Converter<JsonNode, StringResult> {

    context(provider: Provider)
    override fun convert(value: JsonNode) = StringResult(contentType = "application/json", value = value.toString())
}
