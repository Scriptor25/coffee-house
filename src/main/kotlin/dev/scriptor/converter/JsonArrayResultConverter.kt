package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.ResultConverter
import dev.scriptor.server.result.StringResult
import org.json.JSONArray

class JsonArrayResultConverter : ResultConverter<JSONArray, StringResult> {

    context(provider: Provider)
    override fun convert(value: JSONArray) = StringResult(contentType = "application/json", value = value.toString())
}
