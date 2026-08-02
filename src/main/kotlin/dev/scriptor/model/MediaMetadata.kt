package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import dev.scriptor.annotation.Unique
import java.nio.file.Path
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Table("media_metadata", ["media", "metadata"])
data class MediaMetadata(

    @Column(type = String::class, table = "media")
    @PrimaryKey
    val id: Uuid,

    @Column(type = String::class, table = "media")
    @Unique
    val path: Path,

    @Column(table = "media")
    val size: Long,

    @Column(table = "media")
    val title: String,

    @Column("created_at", String::class, "media")
    val createdAt: Instant,

    @Column("modified_at", String::class, "media")
    val modifiedAt: Instant,

    @Column(table = "metadata")
    val duration: Long,

    @Column(table = "metadata")
    val bitrate: Long,

    @Column(table = "metadata")
    val width: Int,

    @Column(table = "metadata")
    val height: Int,

    @Column(table = "metadata")
    val framerate: Double,

    @Column("video_codec", table = "metadata")
    val videoCodec: String?,

    @Column("audio_codec", table = "metadata")
    val audioCodec: String?,
)
