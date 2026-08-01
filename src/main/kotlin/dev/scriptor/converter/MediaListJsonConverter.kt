package dev.scriptor.converter

import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter
import org.json.JSONArray
import org.json.JSONObject

class MediaListJsonConverter : Converter<List<Media>, JSONArray> {

    context(provider: Provider)
    override fun convert(value: List<Media>): JSONArray {
        val converter = provider.convert<Media, JSONObject>()!!

        val json = JSONArray()
        for (entry in value) {
            json.put(converter.convert(entry))
        }
        return json
    }
}
