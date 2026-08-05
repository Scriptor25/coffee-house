package dev.scriptor.model

import dev.scriptor.annotation.*
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media")
data class Media(

    @Column(type = String::class)
    @PrimaryKey
    val id: Uuid,

    @Column(type = String::class)
    @Unique
    val path: Path,

    @Column
    val size: Long,

    @Column
    val title: String,

    @Column("created_at", String::class)
    val createdAt: Instant,

    @Column("modified_at", String::class)
    val modifiedAt: Instant,

    @Column
    val duration: Long,

    @Column
    @ForeignKey
    val video: List<VideoTrack>,

    @Column
    @ForeignKey
    val audio: List<AudioTrack>,

    @Column
    @ForeignKey
    val subtitles: List<SubtitleTrack>,
)
