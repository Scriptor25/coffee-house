package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.show.Show
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class ShowJsonConverter : Converter<Show, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Show): JsonNode = db { value.toJson() }
}
