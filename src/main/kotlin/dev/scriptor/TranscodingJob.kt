package dev.scriptor

import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.VideoTrack
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.*

data class TranscodingJob(
    val group: ThreadGroup,
    val ffmpeg: String,
    val metadata: Media,
    val video: VideoTrack?,
    val cache: Path,
    val variants: List<Variant>,
    val enable: Boolean,
    val device: String?,
    val pipeline: Pipeline,
) {
    private enum class State {
        CREATED,
        WAITING,
        PRE_PROCESSING,
        PROCESSING,
        FINISHED,
        FAILED,
    }

    private companion object {
        var next = 0L
    }

    private val id = next++
    private val lock = Object()

    @Volatile
    private var state: State = State.CREATED

    @Volatile
    private lateinit var work: Path

    context(_: Logger)
    fun master(): Path =
        waitFor(cache / "master.m3u8")

    context(_: Logger)
    fun index(name: String): Path =
        waitFor(cache / name / "index.m3u8")

    context(_: Logger)
    fun segment(name: String, segment: String): Path =
        waitFor(cache / name / "$segment.mp4")

    context(_: Logger)
    private fun run() {
        cache.createDirectories()

        state = State.PRE_PROCESSING
        if (!preProcess()) {
            state = State.FAILED
            return
        }

        state = State.PROCESSING
        if (!process()) {
            state = State.FAILED
            return
        }

        state = State.FINISHED
    }

    @Synchronized
    context(_: Logger)
    private fun start() {
        if (state != State.CREATED)
            return

        state = State.WAITING

        Thread(group) {
            db { run() }

            synchronized(lock) {
                lock.notify()
            }
        }.start()
    }

    context(_: Logger)
    private fun waitFor(path: Path): Path {
        while (path.notExists()) {
            when (state) {
                State.CREATED -> start()

                State.WAITING,
                State.PRE_PROCESSING,
                State.PROCESSING -> {
                    synchronized(lock) {
                        lock.wait()
                    }
                }

                State.FINISHED -> break
                State.FAILED -> error("transcoding job $id failed")
            }
        }

        if (path.notExists()) {
            error("file $path does not exist")
        }

        return path
    }

    context(log: Logger)
    private fun preProcess(): Boolean {
        work = createTempDirectory()

        if (metadata.subtitles.empty())
            return true

        val command = listOf(
            "seconv",
            metadata.path.absolutePathString(),
            "subrip",
            "--ocr-engine:tesseract",
            "--output-folder:${work.absolutePathString()}",
            "--overwrite"
        )

        val code = start(command, "preprocess-$id").waitFor()
        if (code != 0) {
            return false
        }

        val paths = work
            .listDirectoryEntries()
            .filter { path -> path.isRegularFile() && path.name.endsWith(".srt", ignoreCase = true) }
            .sorted()

        return true
    }

    context(log: Logger)
    private fun process(): Boolean {
        val outputs = outputs(metadata) {
            if (video != null) {
                video(video) {
                    variants.forEach {
                        when (it) {
                            is OriginalVariant -> {
                                if (enable) {
                                    transcode(it.name)
                                } else {
                                    copy(it.name)
                                }
                            }

                            is ScaleVariant -> {
                                if (enable) {
                                    transcode(
                                        it.name,
                                        it.profile,
                                        it.bitrate,
                                        it.width,
                                        it.height,
                                    )
                                } else {
                                    copy(
                                        it.name,
                                        it.width,
                                        it.height,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            metadata.audio.forEach {
                audio(it) {
                    if (enable) {
                        transcode()
                    } else {
                        copy()
                    }
                }
            }

            metadata.subtitles.forEach {
                when (it.codec.id.value) {
                    CodecId("subrip"),
                    CodecId("ass"),
                    CodecId("ssa"),
                    CodecId("webvtt") -> Unit

                    else -> {
                        // TODO: require bitmap-to-text first
                    }
                }

                subtitle(it) {
                    if (enable) {
                        transcode()
                    } else {
                        copy()
                    }
                }
            }
        }

        val command = CommandBuilder(
            ffmpeg,
            enable,
            device,
            metadata.path,
            cache,
            outputs,
            pipeline,
        ).build()

        val code = start(command, "process-$id").waitFor()
        if (code != 0) {
            log.warning("process $id failed with exit code $code")
            return false
        }

        return true
    }
}
