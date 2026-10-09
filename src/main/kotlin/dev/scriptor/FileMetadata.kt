package dev.scriptor

import dev.scriptor.model.ffmpeg.Codec
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.media.*
import dev.scriptor.server.Provider
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.time.toKotlinInstant

@JsonSerializable
data class FormatTagsNode(
    @all:JsonProperty
    val title: String? = null,
)

@JsonSerializable
data class FormatNode(
    @all:JsonProperty
    val size: String,
    @all:JsonProperty
    val duration: String,
    @all:JsonProperty
    val tags: FormatTagsNode = FormatTagsNode(),
)

@JsonSerializable
data class StreamTagsNode(
    @all:JsonProperty
    val title: String? = null,
    @all:JsonProperty
    val language: String? = null,
    @all:JsonProperty
    val filename: String? = null,
    @all:JsonProperty
    val mimetype: String? = null,
)

@JsonSerializable
data class StreamDispositionNode(
    @all:JsonProperty
    val default: Number? = null,
    @all:JsonProperty
    val forced: Number? = null,
)

@JsonSerializable
data class StreamNode(
    @all:JsonProperty
    val index: Number,
    @all:JsonProperty("codec_type")
    val codecType: String,
    @all:JsonProperty("codec_name")
    val codecName: String? = null,
    @all:JsonProperty
    val width: Number? = null,
    @all:JsonProperty
    val height: Number? = null,
    @all:JsonProperty("bit_rate")
    val bitRate: String? = null,
    @all:JsonProperty("avg_frame_rate")
    val avgFrameRate: String? = null,
    @all:JsonProperty
    val profile: String? = null,
    @all:JsonProperty
    val level: Number? = null,
    @all:JsonProperty("sample_rate")
    val sampleRate: String? = null,
    @all:JsonProperty
    val channels: Number? = null,
    @all:JsonProperty
    val tags: StreamTagsNode = StreamTagsNode(),
    @all:JsonProperty
    val disposition: StreamDispositionNode = StreamDispositionNode(),
)

@JsonSerializable
data class ChapterTagsNode(
    @all:JsonProperty
    val title: String? = null,
    @all:JsonProperty
    val language: String? = null,
)

@JsonSerializable
data class ChapterNode(
    @all:JsonProperty("start_time")
    val startTime: String,
    @all:JsonProperty("end_time")
    val endTime: String,
    @all:JsonProperty
    val tags: ChapterTagsNode = ChapterTagsNode(),
)

@JsonSerializable
data class MetadataNode(
    @all:JsonProperty
    val format: FormatNode,
    @all:JsonProperty
    val streams: List<StreamNode>,
    @all:JsonProperty
    val chapters: List<ChapterNode>,
)

private fun parseFrameRate(value: String?): Double {
    if (value == null || value == "0/0") return 0.0

    val (num, den) = value
        .split("/")
        .map(String::toDouble)

    return if (den == 0.0) 0.0 else num / den
}

context(
    _: Provider,
    log: Logger,
)
fun getFileMetadata(
    ffprobe: String,
    path: Path,
) {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

    val createdAt = attributes.creationTime().toInstant().toKotlinInstant()
    val modifiedAt = attributes.lastModifiedTime().toInstant().toKotlinInstant()

    val process = start(
        ffprobe,
        "-hide_banner",
        "-loglevel", "error",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        "-show_chapters",
        path.absolutePathString(),
    )

    val json = process.inputStream.reader().readText()

    val value = process.waitFor()
    if (value != 0) {
        db {
            Media.new {
                this.path = path
                this.size = 0
                this.title = path.name
                this.createdAt = createdAt
                this.modifiedAt = modifiedAt
                this.duration = 0.0
            }
        }

        log.warning("failed to get metadata for $path")
        return
    }

    val node: MetadataNode = parseJson(json).fromJson()

    val size = node.format.size.toLong()
    val duration = node.format.duration.toDouble()
    val title = node.format.tags.title ?: path.nameWithoutExtension

    val media = db {
        val it = Media
            .find(MediaTable.path eq path)
            .firstOrNull()
            ?: Media.new {
                this.path = path
            }

        it.size = size
        it.title = title
        it.createdAt = createdAt
        it.modifiedAt = modifiedAt
        it.duration = duration

        it
    }

    for (stream in node.streams) {
        val index = stream.index.toInt()

        val codecType = stream.codecType.lowercase()

        val codec = stream.codecName?.lowercase()

        when (codecType) {
            "video" -> {
                requireNotNull(codec)

                val width = stream.width?.toInt()
                val height = stream.height?.toInt()
                val bitRate = stream.bitRate?.toLongOrNull() ?: 0L
                val frameRate = parseFrameRate(stream.avgFrameRate)
                val profile = stream.profile
                val level = stream.level?.toInt()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1

                db {
                    val it = VideoTrack
                        .find(
                            (VideoTrackTable.media eq media.id)
                                    and (VideoTrackTable.index eq index)
                        )
                        .firstOrNull()
                        ?: VideoTrack.new {
                            this.media = media
                            this.index = index
                        }

                    it.codec = Codec[CodecId(codec)]
                    it.width = width!!
                    it.height = height!!
                    it.bitRate = if (bitRate == 0L)
                        size * 8L * 1000L / (duration * 1000.0).toLong()
                    else bitRate
                    it.frameRate = frameRate
                    it.profile = profile
                    it.level = level
                    it.language = language
                    it.title = title
                    it.default = default
                }
            }

            "audio" -> {
                requireNotNull(codec)

                val bitRate = stream.bitRate?.toLongOrNull() ?: 0L
                val sampleRate = stream.sampleRate?.toLong()
                val channels = stream.channels?.toInt()

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                db {
                    val it = AudioTrack
                        .find(
                            (AudioTrackTable.media eq media.id)
                                    and (AudioTrackTable.index eq index)
                        )
                        .firstOrNull()
                        ?: AudioTrack.new {
                            this.media = media
                            this.index = index
                        }

                    it.codec = Codec[CodecId(codec)]
                    it.bitRate = bitRate
                    it.sampleRate = sampleRate!!
                    it.channels = channels!!
                    it.language = language
                    it.title = title
                    it.default = default
                    it.forced = forced
                }
            }

            "subtitle" -> {
                requireNotNull(codec)

                val language = stream.tags.language
                val title = stream.tags.title

                val default = stream.disposition.default?.toInt() == 1
                val forced = stream.disposition.forced?.toInt() == 1

                db {
                    val it = SubtitleTrack
                        .find(
                            (SubtitleTrackTable.media eq media.id)
                                    and (SubtitleTrackTable.index eq index)
                        )
                        .firstOrNull()
                        ?: SubtitleTrack.new {
                            this.media = media
                            this.index = index
                        }

                    it.codec = Codec[CodecId(codec)]
                    it.language = language
                    it.title = title
                    it.default = default
                    it.forced = forced
                }
            }

            "attachment" -> {
                val filename = stream.tags.filename
                val mimetype = stream.tags.mimetype

                // TODO: Attachment.new { ... }
            }

            else -> Unit
        }
    }

    var i = 0
    for (chapter in node.chapters) {
        val index = i++

        val start = chapter.startTime.toDouble()
        val end = chapter.endTime.toDouble()

        val language = chapter.tags.language
        val title = chapter.tags.title

        db {
            val it = Chapter
                .find(
                    (ChapterTable.media eq media.id)
                            and (ChapterTable.index eq index)
                )
                .firstOrNull()
                ?: Chapter.new {
                    this.media = media
                    this.index = index
                }

            it.start = start
            it.end = end
            it.language = language
            it.title = title
        }
    }
}

context(
    _: Provider,
    log: Logger,
)
fun getFileMetadata(
    ffprobe: String,
    paths: List<Path>,
) {
    val existing = db {
        MediaTable
            .select(MediaTable.path)
            .map { it[MediaTable.path] }
            .toSet()
    }

    val revalidate = paths.filterNot { it in existing }

    val executor = Executors.newFixedThreadPool(
        minOf(
            4,
            Runtime.getRuntime().availableProcessors(),
        ),
    )

    val tasks = mutableListOf<Callable<Unit>>()

    for ((index, path) in revalidate.withIndex()) {
        tasks.add {
            log.info("${index + 1} / ${revalidate.size}")

            getFileMetadata(
                ffprobe,
                path,
            )
        }
    }

    val results = executor.invokeAll(tasks)

    executor.shutdown()

    for (result in results) {
        result.get()
    }
}
