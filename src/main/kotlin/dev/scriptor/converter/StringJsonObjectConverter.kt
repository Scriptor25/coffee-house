package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import org.json.JSONObject

class StringJsonObjectConverter : Converter<String, JSONObject> {

    override fun convert(value: String): JSONObject = JSONObject(value)
}
