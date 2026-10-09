package dev.scriptor

import dev.scriptor.model.Transcoding
import dev.scriptor.model.TranscodingState
import dev.scriptor.model.TranscodingTable
import dev.scriptor.model.ffmpeg.CodecId
import dev.scriptor.model.media.Media
import dev.scriptor.model.media.VideoTrack
import org.jetbrains.exposed.v1.core.eq
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.time.Clock

sealed interface TranscodingTask {

    val media: Media
    val path: Path

    context(_: Logger)
    fun master(): Path? =
        waitFor(path / "master.m3u8")

    context(_: Logger)
    fun index(name: String): Path? =
        waitFor(path / name / "index.m3u8")

    context(_: Logger)
    fun segment(name: String, segment: String): Path? =
        waitFor(path / name / "$segment.mp4")

    context(log: Logger)
    fun waitFor(path: Path): Path?
}

data class CachedTranscodingTask(
    override val media: Media,
    override val path: Path,
) : TranscodingTask {

    context(log: Logger)
    override fun waitFor(path: Path): Path? {
        db {
            Transcoding.findSingleByAndUpdate(TranscodingTable.media eq media.id) {
                it.access = Clock.System.now()
            }
        }

        if (path.exists())
            return path
        return null
    }
}

data class NewTranscodingTask(
    override val media: Media,
    override val path: Path,
    val group: ThreadGroup,
    val video: VideoTrack?,
    val variants: List<Variant>,
    val ffmpeg: String,
    val enable: Boolean,
    val device: String?,
    val pipeline: Pipeline,
) : TranscodingTask {

    private companion object {
        var next = 0L

        val bitmap = setOf(
            CodecId("dvb_subtitle"),
            CodecId("dvd_subtitle"),
            CodecId("hdmv_pgs_subtitle"),
            CodecId("xsub"),
        )

        val text = setOf(
            CodecId("ass"),
            CodecId("ssa"),
            CodecId("jacosub"),
            CodecId("microdvd"),
            CodecId("mov_text"),
            CodecId("mpl2"),
            CodecId("pjs"),
            CodecId("realtext"),
            CodecId("sami"),
            CodecId("srt"),
            CodecId("subrip"),
            CodecId("subviewer"),
            CodecId("subviewer1"),
            CodecId("text"),
            CodecId("ttml"),
            CodecId("vplayer"),
            CodecId("webvtt"),
        )
    }

    val id = next++

    @Volatile
    var state: TranscodingState = TranscodingState.CREATED

    context(log: Logger)
    override fun waitFor(path: Path): Path? {
        while (state == TranscodingState.CREATED || path.notExists()) {
            when (state) {
                TranscodingState.CREATED -> start()
                TranscodingState.PROCESSING -> Thread.sleep(100)
                TranscodingState.FINISHED -> break
                TranscodingState.FAILED -> {
                    log.warning("transcoding task $id failed")
                    return null
                }
            }
        }

        if (path.exists())
            return path
        return null
    }

    context(_: Logger)
    private fun run() {
        if (!process()) {
            state = TranscodingState.FAILED
            return
        }

        state = TranscodingState.FINISHED
    }

    @OptIn(ExperimentalPathApi::class)
    @Synchronized
    context(_: Logger)
    private fun start() {
        if (state != TranscodingState.CREATED)
            return

        if (path.exists())
            path.deleteRecursively()
        path.createDirectories()

        state = TranscodingState.PROCESSING

        Thread(group) { run() }.start()
    }

    context(log: Logger)
    private fun process(): Boolean {
        val command = db {
            val outputs = outputs(media) {
                if (video != null) {
                    video(video) {
                        variants.forEach {
                            when (it) {
                                is OriginalVariant ->
                                    if (enable)
                                        transcode(it.name)
                                    else
                                        copy(it.name)

                                is ScaleVariant ->
                                    if (enable)
                                        transcode(
                                            it.name,
                                            it.profile,
                                            it.bitrate,
                                            it.width,
                                            it.height,
                                        )
                                    else
                                        copy(
                                            it.name,
                                            it.width,
                                            it.height,
                                        )
                            }
                        }
                    }
                }

                media.audio.forEach {
                    audio(it) {
                        if (enable)
                            transcode()
                        else
                            copy()
                    }
                }

                media.subtitles
                    .filter { it.codec.id.value in text }
                    .forEach {
                        subtitle(it) {
                            if (enable)
                                transcode()
                            else
                                copy()
                        }
                    }
            }

            CommandBuilder(
                ffmpeg,
                enable,
                device,
                media.path,
                path,
                outputs,
                pipeline,
            ).build()
        }

        val code = start(command, "process-$id").waitFor()
        if (code != 0) {
            log.warning("process-$id failed with exit code $code")
            return false
        }

        return true
    }
}
