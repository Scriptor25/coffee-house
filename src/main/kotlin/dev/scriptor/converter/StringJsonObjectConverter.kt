package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.json.JSONObject

class StringJsonObjectConverter : Converter<String, JSONObject> {

    context(provider: Provider)
    override fun convert(value: String): JSONObject = JSONObject(value)
}
