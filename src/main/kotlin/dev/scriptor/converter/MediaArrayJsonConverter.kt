package dev.scriptor.converter

import dev.scriptor.model.Media
import dev.scriptor.server.converter.Converter
import org.json.JSONArray

class MediaArrayJsonConverter : Converter<Array<Media>, JSONArray> {

    val converter = MediaJsonConverter()

    override fun convert(value: Array<Media>): JSONArray {
        val json = JSONArray()
        for (entry in value) {
            json.put(converter.convert(entry))
        }
        return json
    }
}
