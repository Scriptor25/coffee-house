package dev.scriptor.converter

import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.movie.Movie
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MovieJsonConverter : Converter<Movie, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: Movie): JsonObjectNode {
        val database = provider.getContextT<Database>()
        return jsonOf(
            "title" to jsonOf(value.title),
            "items" to provider(transaction(database) { value.items.map { it.id.value }.toList() }),
            // TODO: tmdb data
        )
    }
}
