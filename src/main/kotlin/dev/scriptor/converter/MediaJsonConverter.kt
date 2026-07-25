package dev.scriptor.converter

import dev.scriptor.model.Media
import dev.scriptor.server.converter.Converter
import org.json.JSONObject
import kotlin.io.path.absolutePathString

class MediaJsonConverter : Converter<Media, JSONObject> {

    override fun convert(value: Media): JSONObject {
        val json = JSONObject()
        json.put("id", value.id.toHexDashString())
        json.put("path", value.path.absolutePathString())
        json.put("title", value.title)
        json.put("created_at", value.createdAt.toString())
        json.put("modified_at", value.modifiedAt.toString())
        return json
    }
}
