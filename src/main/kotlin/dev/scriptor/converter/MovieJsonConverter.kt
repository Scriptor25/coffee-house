package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.db
import dev.scriptor.model.movie.Movie
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson

class MovieJsonConverter : Converter<Movie, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: Movie): JsonNode = db { value.toJson() }
}
