package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter
import org.json.JSONArray
import org.json.JSONObject

class ListJsonConverter : Converter<List<*>, JSONArray> {

    context(provider: Provider)
    override fun convert(value: List<*>): JSONArray {
        val converter = provider.convert<Any, JSONObject>()!!

        val json = JSONArray()
        for (entry in value) {
            json.put(converter.convert(entry!!))
        }
        return json
    }
}
