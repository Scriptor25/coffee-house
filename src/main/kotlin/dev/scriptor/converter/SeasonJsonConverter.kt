package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.show.Season
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SeasonJsonConverter : Converter<Season, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: Season): JsonObjectNode {
        val database = provider.getContextT<Database>()
        return jsonOf(
            "index" to jsonOf(value.index),
            "episodes" to provider(transaction(database) { value.episodes.toList() }),
        )
    }
}
