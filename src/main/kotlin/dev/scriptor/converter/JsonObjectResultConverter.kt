package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultString
import org.json.JSONObject

class JsonObjectResultConverter : Converter<JSONObject, HTTPResult<*>> {

    override fun convert(value: JSONObject): HTTPResult<*> = HTTPResultString(value = value.toString())
}
