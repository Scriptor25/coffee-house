package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MediaJsonConverter : Converter<Media, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Media): JsonNode {
        val database = provider.getContextT<Database>()
            ?: error("missing database context")

        lateinit var video: JsonNode
        lateinit var audio: JsonNode
        lateinit var subtitles: JsonNode
        lateinit var chapters: JsonNode

        transaction(database) {
            video = provider(value.video.toList())
            audio = provider(value.audio.toList())
            subtitles = provider(value.subtitles.toList())
            chapters = provider(value.chapters.toList())
        }

        return jsonOf(
            "id" to jsonOf(value.id),
            "path" to jsonOf(value.path),
            "size" to jsonOf(value.size),
            "title" to jsonOf(value.title),
            "created_at" to jsonOf(value.createdAt),
            "modified_at" to jsonOf(value.modifiedAt),
            "duration" to jsonOf(value.duration),
            "video" to video,
            "audio" to audio,
            "subtitles" to subtitles,
            "chapters" to chapters,
        )
    }
}
