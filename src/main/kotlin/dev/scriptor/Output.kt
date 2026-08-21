package dev.scriptor

import dev.scriptor.encoder.audio.AudioEncoder
import dev.scriptor.encoder.subtitle.SubtitleEncoder
import dev.scriptor.encoder.video.VideoEncoder
import dev.scriptor.model.media.AudioTrack
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.SubtitleTrack
import dev.scriptor.model.media.VideoTrack

sealed interface VideoEncoding {

    operator fun invoke(index: Int, encoder: VideoEncoder): List<String>

    data object Copy : VideoEncoding {

        override fun invoke(index: Int, encoder: VideoEncoder): List<String> =
            listOf("-c:v:$index", "copy")
    }

    data class Transcode(
        val profile: Profile,
        val bitrate: Long,
    ) : VideoEncoding {

        override fun invoke(index: Int, encoder: VideoEncoder): List<String> = buildList {
            this += listOf("-c:v:$index", "${encoder.id}")
            this += encoder(index, profile, bitrate)
        }
    }
}

sealed interface AudioEncoding {

    operator fun invoke(index: Int, encoder: AudioEncoder): List<String>

    data object Copy : AudioEncoding {

        override fun invoke(index: Int, encoder: AudioEncoder): List<String> =
            listOf("-c:a:$index", "copy")
    }

    data object Transcode : AudioEncoding {

        override fun invoke(index: Int, encoder: AudioEncoder): List<String> =
            listOf("-c:a:$index", "${encoder.id}") + encoder(index)
    }
}

sealed interface SubtitleEncoding {

    operator fun invoke(index: Int, encoder: SubtitleEncoder): List<String>

    data object Copy : SubtitleEncoding {

        override fun invoke(index: Int, encoder: SubtitleEncoder): List<String> =
            listOf("-c:s:$index", "copy")
    }

    data object Transcode : SubtitleEncoding {

        override fun invoke(index: Int, encoder: SubtitleEncoder): List<String> =
            listOf("-c:s:$index", "${encoder.id}") + encoder(index)
    }
}

sealed interface Output {
    val name: String
    val source: Int
}

data class VideoOutput(
    override val name: String,
    override val source: Int,
    val encoding: VideoEncoding,
    val scaled: Boolean,
    val width: Int,
    val height: Int,
) : Output

data class AudioOutput(
    override val name: String,
    override val source: Int,
    val encoding: AudioEncoding,
    val language: String?,
    val title: String?,
    val default: Boolean,
) : Output

data class SubtitleOutput(
    override val name: String,
    override val source: Int,
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

    fun copy(name: String = "original") {
        outputs += VideoOutput(
            name,
            source,
            VideoEncoding.Copy,
            false,
            metadata.width,
            metadata.height,
        )
    }

    fun transcode(
        name: String = "original",
        profile: Profile = Profile.ARCHIVAL,
        bitrate: Long = metadata.bitRate,
    ) {
        outputs += VideoOutput(
            name,
            source,
            VideoEncoding.Transcode(profile, bitrate),
            false,
            metadata.width,
            metadata.height,
        )
    }

    fun copy(
        name: String,
        width: Int,
        height: Int,
    ) {
        outputs += VideoOutput(
            name,
            source,
            VideoEncoding.Copy,
            true,
            width,
            height,
        )
    }

    fun transcode(
        name: String,
        profile: Profile,
        bitrate: Long,
        width: Int,
        height: Int,
    ) {
        outputs += VideoOutput(
            name,
            source,
            VideoEncoding.Transcode(profile, bitrate),
            true,
            width,
            height,
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
            AudioEncoding.Copy,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun transcode(name: String = "audio$source") {
        output = AudioOutput(
            name,
            source,
            AudioEncoding.Transcode,
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
            SubtitleEncoding.Copy,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun transcode(name: String = "subtitle$source") {
        output = SubtitleOutput(
            name,
            source,
            SubtitleEncoding.Transcode,
            metadata.language,
            metadata.title,
            metadata.default,
        )
    }

    fun build(): List<SubtitleOutput> = listOfNotNull(output)
}
