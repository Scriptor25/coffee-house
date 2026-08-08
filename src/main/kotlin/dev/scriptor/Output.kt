package dev.scriptor

import dev.scriptor.model.AudioTrack
import dev.scriptor.model.Media
import dev.scriptor.model.SubtitleTrack
import dev.scriptor.model.VideoTrack

sealed interface Output {
    val name: String
}

sealed interface VideoCodec {
    data object Copy : VideoCodec
    data class H264(
        val profile: Profile,
        val bitrate: Long,
    ) : VideoCodec
}

sealed interface AudioCodec {
    data object Copy : AudioCodec
    data object Aac : AudioCodec
}

sealed interface SubtitleCodec {
    data object Copy : SubtitleCodec
    data object WebVtt : SubtitleCodec
}

data class VideoOutput(
    override val name: String,

    val scaled: Boolean,
    val source: Int,
    val width: Int,
    val height: Int,
    val codec: VideoCodec,
) : Output

data class AudioOutput(
    override val name: String,

    val source: Int,
    val codec: AudioCodec,
    val language: String?,
    val title: String?,
    val default: Boolean,
) : Output

data class SubtitleOutput(
    override val name: String,

    val source: Int,
    val codec: SubtitleCodec,
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
            VideoCodec.Copy,
        )
    }

    fun originalH264(
        name: String = "original",
        profile: Profile = Profile.LOSSLESS,
        bitrate: Long = metadata.bitRate,
    ) {
        outputs += VideoOutput(
            name,
            false,
            source,
            metadata.width,
            metadata.height,
            VideoCodec.H264(profile, bitrate),
        )
    }

    fun scaleCopy(
        name: String,
        width: Int,
        height: Int,
    ) {
        outputs += VideoOutput(
            name,
            true,
            source,
            width,
            height,
            VideoCodec.Copy,
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
            VideoCodec.H264(profile, bitrate),
        )
    }

    fun build(): List<VideoOutput> = outputs
}

class AudioQuery(
    private val metadata: AudioTrack,
    private val source: Int,
) {
    private var output: AudioOutput? = null

    fun copy(name: String = metadata.language ?: "audio$source") {
        output = AudioOutput(
            name,
            source,
            AudioCodec.Copy,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun aac(name: String = metadata.language ?: "audio$source") {
        output = AudioOutput(
            name,
            source,
            AudioCodec.Aac,
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

    fun copy(name: String = metadata.language ?: "subtitle$source") {
        output = SubtitleOutput(
            name,
            source,
            SubtitleCodec.Copy,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun webVtt(name: String = metadata.language ?: "subtitle$source") {
        output = SubtitleOutput(
            name,
            source,
            SubtitleCodec.WebVtt,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun build(): List<SubtitleOutput> = listOfNotNull(output)
}
