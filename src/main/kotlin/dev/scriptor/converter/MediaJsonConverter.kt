package dev.scriptor.converter

import dev.scriptor.model.AudioTrack
import dev.scriptor.model.Media
import dev.scriptor.model.SubtitleTrack
import dev.scriptor.model.VideoTrack
import dev.scriptor.server.Provider
import dev.scriptor.server.convert
import dev.scriptor.server.converter.Converter
import org.json.JSONObject
import kotlin.io.path.absolutePathString

class MediaJsonConverter : Converter<Media, JSONObject> {

    context(provider: Provider)
    override fun convert(value: Media): JSONObject {
        val videoConverter = provider.convert<List<VideoTrack>, JSONObject>()!!
        val audioConverter = provider.convert<List<AudioTrack>, JSONObject>()!!
        val subtitlesConverter = provider.convert<List<SubtitleTrack>, JSONObject>()!!

        val json = JSONObject()
        json.put("id", value.id.toHexDashString())
        json.put("path", value.path.absolutePathString())
        json.put("size", value.size)
        json.put("title", value.title)
        json.put("created_at", value.createdAt.toString())
        json.put("modified_at", value.modifiedAt.toString())
        json.put("duration", value.duration.toString())
        json.put("video", videoConverter.convert(value.video))
        json.put("audio", audioConverter.convert(value.audio))
        json.put("subtitles", subtitlesConverter.convert(value.subtitles))
        return json
    }
}
