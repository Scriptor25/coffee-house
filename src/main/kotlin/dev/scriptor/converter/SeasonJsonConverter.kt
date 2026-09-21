package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.show.Season
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class SeasonJsonConverter : Converter<Season, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Season): JsonNode = db { value.toJson() }
}
