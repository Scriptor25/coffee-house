package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.model.media.VideoTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.toJson
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class VideoTrackJsonConverter : Converter<VideoTrack, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: VideoTrack): JsonNode {
        val database: Database = provider?.getT()
            ?: error("missing database")
        return transaction(database) { value.toJson() }
    }
}
