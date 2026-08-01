package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.ResultConverter
import dev.scriptor.server.result.StringResult
import org.json.JSONObject

class JsonObjectResultConverter : ResultConverter<JSONObject, StringResult> {

    context(provider: Provider)
    override fun convert(value: JSONObject) = StringResult(contentType = "application/json", value = value.toString())
}
