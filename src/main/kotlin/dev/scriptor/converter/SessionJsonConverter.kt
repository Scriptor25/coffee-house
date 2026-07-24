package dev.scriptor.converter

import dev.scriptor.model.Session
import dev.scriptor.server.converter.Converter
import org.json.JSONObject

class SessionJsonConverter : Converter<Session, JSONObject> {

    override fun convert(value: Session): JSONObject {
        val json = JSONObject()
        json.put("id", value.id)
        json.put("access", value.access)
        json.put("open", value.open)
        json.put("sequence", value.sequence)
        json.put("next", value.next)
        return json
    }
}
