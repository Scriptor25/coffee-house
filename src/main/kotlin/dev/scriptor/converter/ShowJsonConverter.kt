package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.model.show.Show
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ShowJsonConverter : Converter<Show, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Show): JsonNode {
        val database: Database = provider.getT()
            ?: error("missing database")
        return transaction(database) { value.toJson() }
    }
}
