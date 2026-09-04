package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.show.Show
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ShowJsonConverter : Converter<Show, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Show): JsonNode {
        val database = provider.getContextT<Database>()
        return jsonOf(
            "title" to jsonOf(value.title),
            "seasons" to provider(transaction(database) { value.seasons.toList() }),
        )
    }
}
