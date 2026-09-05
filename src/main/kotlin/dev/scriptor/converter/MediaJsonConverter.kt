package dev.scriptor.converter

import dev.scriptor.JsonArrayNode
import dev.scriptor.JsonObjectNode
import dev.scriptor.jsonOf
import dev.scriptor.model.media.Media
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class MediaJsonConverter : Converter<Media, JsonObjectNode> {

    context(provider: Provider)
    override fun convert(value: Media): JsonObjectNode {
        val database = provider.getContextT<Database>()
            ?: error("missing database context")

        lateinit var video: JsonArrayNode
        lateinit var audio: JsonArrayNode
        lateinit var subtitles: JsonArrayNode
        lateinit var chapters: JsonArrayNode

        transaction(database) {
            video = provider(value.video.toList())
            audio = provider(value.audio.toList())
            subtitles = provider(value.subtitles.toList())
            chapters = provider(value.chapters.toList())
        }

        return jsonOf(
            "id" to jsonOf(value.id.toString()),
            "path" to jsonOf(value.path.toString()),
            "size" to jsonOf(value.size),
            "title" to jsonOf(value.title),
            "created_at" to jsonOf(value.createdAt.toEpochMilliseconds()),
            "modified_at" to jsonOf(value.modifiedAt.toEpochMilliseconds()),
            "duration" to jsonOf(value.duration),
            "video" to video,
            "audio" to audio,
            "subtitles" to subtitles,
            "chapters" to chapters,
        )
    }
}
