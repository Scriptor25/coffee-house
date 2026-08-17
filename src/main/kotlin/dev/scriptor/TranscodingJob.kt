package dev.scriptor

import dev.scriptor.model.Media
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

data class TranscodingJob(
    val ffmpeg: String,
    val metadata: Media,
    val cache: Path,
    val variants: List<Variant>,
    val requirements: TranscodingRequirements,
    val pipeline: Pipeline,
) {
    private enum class State {
        CREATED,
        RUNNING,
        FINISHED,
        FAILED,
    }

    private companion object {
        var next = 0L
    }

    private val id = next++
    private val command = buildCommand()

    @Volatile
    private var state: State = State.CREATED

    @Volatile
    private lateinit var process: Process

    context(_: Logger)
    fun master(): Path =
        waitFor(cache.resolve("master.m3u8"))

    context(_: Logger)
    fun index(name: String): Path =
        waitFor(cache.resolve(name).resolve("index.m3u8"))

    context(_: Logger)
    fun segment(name: String, segment: String): Path =
        waitFor(cache.resolve(name).resolve("$segment.mp4"))

    @Synchronized
    context(parent: Logger)
    private fun start() {
        if (state == State.RUNNING || state == State.FINISHED) return

        state = State.RUNNING

        cache.createDirectories()

        process = start(command, "ffmpeg-$id")
    }

    context(_: Logger)
    private fun waitFor(path: Path): Path {
        while (path.notExists()) {
            if (state == State.RUNNING && !process.isAlive) {
                state =
                    if (process.exitValue() == 0)
                        State.FINISHED
                    else State.FAILED
            }

            when (state) {
                State.CREATED -> start()
                State.RUNNING -> Thread.sleep(10)
                State.FINISHED -> break
                State.FAILED -> error("transcoding job $id failed")
            }
        }

        if (path.notExists()) {
            error("file $path does not exist")
        }

        return path
    }

    private fun buildCommand(): List<String> {
        val outputs = outputs(metadata) {
            video(0) {
                variants.forEach {
                    when (it) {
                        is OriginalVariant -> {
                            if (requirements.enable) {
                                transcode(
                                    it.name,
                                    requirements.video,
                                )
                            } else {
                                copy(it.name)
                            }
                        }

                        is ScaleVariant -> {
                            if (requirements.enable) {
                                transcode(
                                    it.name,
                                    requirements.video,
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

            metadata.audio.forEach {
                audio(it) {
                    if (requirements.enable) {
                        transcode(codec = requirements.audio)
                    } else {
                        copy()
                    }
                }
            }

            metadata.subtitles.filter {
                // TODO: HLS does not support bitmap subtitles?
                when (it.codec) {
                    "subrip",
                    "ass",
                    "ssa",
                    "webvtt" -> true

                    else -> false
                }
            }.forEach {
                subtitle(it) {
                    if (requirements.enable) {
                        transcode(codec = requirements.subtitle)
                    } else {
                        copy()
                    }
                }
            }
        }

        return CommandBuilder(
            ffmpeg,
            requirements.enable,
            requirements.device,
            metadata.path,
            cache,
            outputs,
            pipeline,
        ).build()
    }
}
