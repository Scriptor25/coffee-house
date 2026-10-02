package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.jsonOf
import dev.scriptor.model.ffmpeg.Codec
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class CodecJsonConverter : Converter<Codec, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Codec): JsonNode = db { jsonOf(value.id.toString()) }
}
