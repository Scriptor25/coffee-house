package dev.scriptor.converter

import dev.scriptor.model.Media
import dev.scriptor.server.converter.Converter
import org.json.JSONArray

class MediaListJsonConverter : Converter<List<Media>, JSONArray> {

    val converter = MediaJsonConverter()

    override fun convert(value: List<Media>): JSONArray {
        val json = JSONArray()
        for (entry in value) {
            json.put(converter.convert(entry))
        }
        return json
    }
}
