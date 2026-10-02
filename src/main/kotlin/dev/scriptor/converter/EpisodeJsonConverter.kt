package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.show.Episode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class EpisodeJsonConverter : Converter<Episode, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Episode): JsonNode = db { value.toJson() }
}
