package dev.scriptor.converter

import dev.scriptor.model.Media
import dev.scriptor.server.annotation.Conversion
import dev.scriptor.server.converter.ConversionPath
import dev.scriptor.server.converter.Converter
import org.json.JSONArray
import org.json.JSONObject

class MediaListJsonConverter : Converter<List<Media>, JSONArray> {

    @Conversion
    lateinit var converter: ConversionPath<Media, JSONObject>

    override fun convert(value: List<Media>): JSONArray {
        val json = JSONArray()
        for (entry in value) {
            json.put(converter.convert(entry))
        }
        return json
    }
}
