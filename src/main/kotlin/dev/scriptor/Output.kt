package dev.scriptor

import dev.scriptor.model.AudioTrack
import dev.scriptor.model.Media
import dev.scriptor.model.SubtitleTrack
import dev.scriptor.model.VideoTrack

sealed interface Output {
    val name: String
}

sealed interface VideoEncoding {

    val codec: Codec


    data class Parameterized(
        val encoder: Encoder,
        val profile: Profile,
        val bitrate: Long,
    ) : VideoEncoding
}

enum class AudioEncoding {
    COPY,
    AAC,
}

enum class SubtitleEncoding {
    COPY,
    WEBVTT,
}

data class VideoOutput(
    override val name: String,

    val scaled: Boolean,
    val source: Int,
    val width: Int,
    val height: Int,
    val encoding: VideoEncoding,
) : Output

data class AudioOutput(
    override val name: String,

    val source: Int,
    val encoding: AudioEncoding,
    val language: String?,
    val title: String?,
    val default: Boolean,
) : Output

data class SubtitleOutput(
    override val name: String,

    val source: Int,
    val encoding: SubtitleEncoding,
    val language: String?,
    val title: String?,
    val default: Boolean,
) : Output

class OutputQuery(
    private val metadata: Media,
) {
    private val outputs = mutableListOf<Output>()

    fun video(index: Int, block: VideoQuery.() -> Unit) {
        VideoQuery(
            metadata.video.first { it.index == index },
            index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun video(metadata: VideoTrack, block: VideoQuery.() -> Unit) {
        VideoQuery(
            metadata,
            metadata.index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun audio(index: Int, block: AudioQuery.() -> Unit) {
        AudioQuery(
            metadata.audio.first { it.index == index },
            index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun audio(metadata: AudioTrack, block: AudioQuery.() -> Unit) {
        AudioQuery(
            metadata,
            metadata.index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun subtitle(index: Int, block: SubtitleQuery.() -> Unit) {
        SubtitleQuery(
            metadata.subtitles.first { it.index == index },
            index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun subtitle(metadata: SubtitleTrack, block: SubtitleQuery.() -> Unit) {
        SubtitleQuery(
            metadata,
            metadata.index,
        )
            .apply(block)
            .build()
            .also(outputs::addAll)
    }

    fun build(): List<Output> = outputs
}

fun outputs(metadata: Media, block: OutputQuery.() -> Unit): List<Output> = OutputQuery(metadata).apply(block).build()

class VideoQuery(
    private val metadata: VideoTrack,
    private val source: Int,
) {
    private val outputs = mutableListOf<VideoOutput>()

    fun originalCopy(name: String = "original") {
        outputs += VideoOutput(
            name,
            false,
            source,
            metadata.width,
            metadata.height,
            VideoEncoding(Encoder.Copy),
        )
    }

    fun originalH264(
        name: String = "original",
        profile: Profile = Profile.ARCHIVAL,
        bitrate: Long = metadata.bitRate,
    ) {
        outputs += VideoOutput(
            name,
            false,
            source,
            metadata.width,
            metadata.height,
            VideoEncoding.H264(profile, bitrate),
        )
    }

    fun scaleH264(
        name: String,
        width: Int,
        height: Int,
        profile: Profile,
        bitrate: Long,
    ) {
        outputs += VideoOutput(
            name,
            true,
            source,
            width,
            height,
            VideoEncoding.H264(profile, bitrate),
        )
    }

    fun build(): List<VideoOutput> = outputs
}

class AudioQuery(
    private val metadata: AudioTrack,
    private val source: Int,
) {
    private var output: AudioOutput? = null

    fun copy(name: String = "audio$source") {
        output = AudioOutput(
            name,
            source,
            AudioEncoding.COPY,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun aac(name: String = "audio$source") {
        output = AudioOutput(
            name,
            source,
            AudioEncoding.AAC,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun build(): List<AudioOutput> = listOfNotNull(output)
}

class SubtitleQuery(
    private val metadata: SubtitleTrack,
    private val source: Int,
) {
    private var output: SubtitleOutput? = null

    fun copy(name: String = "subtitle$source") {
        output = SubtitleOutput(
            name,
            source,
            SubtitleEncoding.COPY,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun webVtt(name: String = "subtitle$source") {
        output = SubtitleOutput(
            name,
            source,
            SubtitleEncoding.WEBVTT,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun build(): List<SubtitleOutput> = listOfNotNull(output)
}
