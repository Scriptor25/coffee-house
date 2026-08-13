package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.typeOf

class MediaJsonConverter : Converter<Media, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Media): JsonNode {
        val database = provider[typeOf<Database>()] as Database

        val (video, audio, subtitles) = transaction(database) {
            Triple(
                value.video.toList(),
                value.audio.toList(),
                value.subtitles.toList(),
            )
        }

        return jsonOf(
            "id" to jsonOf(value.id),
            "path" to jsonOf(value.path),
            "size" to jsonOf(value.size),
            "title" to jsonOf(value.title),
            "created_at" to jsonOf(value.createdAt),
            "modified_at" to jsonOf(value.modifiedAt),
            "duration" to jsonOf(value.duration),
            "video" to provider<_, JsonNode>(video),
            "audio" to provider<_, JsonNode>(audio),
            "subtitles" to provider<_, JsonNode>(subtitles),
        )
    }
}
