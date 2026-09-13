package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.ffmpeg.Codec
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CodecJsonConverter : Converter<Codec, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Codec): JsonNode {
        val database: Database = provider.getT()
            ?: error("missing database")
        return transaction(database) { jsonOf(value.id.toString()) }
    }
}
