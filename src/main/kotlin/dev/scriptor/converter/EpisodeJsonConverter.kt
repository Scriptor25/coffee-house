package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.show.Episode
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EpisodeJsonConverter : Converter<Episode, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Episode): JsonNode {
        val database = provider.getContextT<Database>()
        return jsonOf(
            "index" to jsonOf(value.index),
            "items" to provider(transaction(database) { value.items.map { it.id.value }.toList() }),
        )
    }
}
