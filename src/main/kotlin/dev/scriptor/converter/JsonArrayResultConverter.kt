package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.result.HTTPResult
import dev.scriptor.server.http.result.HTTPResultString
import org.json.JSONArray

class JsonArrayResultConverter : Converter<JSONArray, HTTPResult<*>> {

    override fun convert(value: JSONArray): HTTPResult<*> = HTTPResultString(value = value.toString())
}
