package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.jsonOf
import dev.scriptor.model.AudioTrack
import dev.scriptor.model.Media
import dev.scriptor.model.SubtitleTrack
import dev.scriptor.model.VideoTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.typeOf

class MediaJsonConverter : Converter<Media, JsonNode> {

    context(provider: Provider)
    override fun convert(value: Media): JsonNode {
        val videoConverter = provider.convert<List<VideoTrack>, JsonNode>()!!
        val audioConverter = provider.convert<List<AudioTrack>, JsonNode>()!!
        val subtitlesConverter = provider.convert<List<SubtitleTrack>, JsonNode>()!!

        val database = provider[typeOf<Database>()] as Database

        return transaction(database) {
            jsonOf(
                "id" to jsonOf(value.id),
                "path" to jsonOf(value.path),
                "size" to jsonOf(value.size),
                "title" to jsonOf(value.title),
                "created_at" to jsonOf(value.createdAt),
                "modified_at" to jsonOf(value.modifiedAt),
                "duration" to jsonOf(value.duration),
                "video" to videoConverter.convert(value.video.toList()),
                "audio" to audioConverter.convert(value.audio.toList()),
                "subtitles" to subtitlesConverter.convert(value.subtitles.toList()),
            )
        }
    }
}
