package dev.scriptor.model

import dev.scriptor.Entity
import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column
    override val id: Uuid,

    @Column(unique = true)
    val path: Path,

    @Column
    val size: Long,

    @Column
    val title: String,

    @Column("created_at")
    val createdAt: Instant,

    @Column("modified_at")
    val modifiedAt: Instant,

    @Column
    val duration: Double,
) : Entity {
    lateinit var video: List<VideoTrack>
    lateinit var audio: List<AudioTrack>
    lateinit var subtitles: List<SubtitleTrack>

    constructor(
        id: Uuid,
        path: Path,
        size: Long,
        title: String,
        createdAt: Instant,
        modifiedAt: Instant,
        duration: Double,
        video: List<VideoTrack>,
        audio: List<AudioTrack>,
        subtitles: List<SubtitleTrack>,
    ) : this(
        id,
        path,
        size,
        title,
        createdAt,
        modifiedAt,
        duration,
    ) {
        this.video = video
        this.audio = audio
        this.subtitles = subtitles
    }
}
