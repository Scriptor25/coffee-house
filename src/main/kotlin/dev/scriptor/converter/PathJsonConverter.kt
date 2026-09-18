package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson
import java.nio.file.Path

class PathJsonConverter : Converter<Path, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Path): JsonNode = value.toString().toJson()
}
