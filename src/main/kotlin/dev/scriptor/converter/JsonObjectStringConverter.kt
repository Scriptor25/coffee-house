package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import org.json.JSONObject

class JsonObjectStringConverter : Converter<JSONObject, String> {

    override fun convert(value: JSONObject): String = value.toString()
}