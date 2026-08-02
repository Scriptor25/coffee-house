package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("metadata")
data class Metadata(

    @Column(type = String::class)
    @PrimaryKey
    @ForeignKey("media", "id")
    val id: Uuid,

    @Column
    val duration: Long,

    @Column
    val bitrate: Long,

    @Column
    val width: Int,

    @Column
    val height: Int,

    @Column
    val framerate: Double,

    @Column("video_codec")
    val videoCodec: String?,

    @Column("audio_codec")
    val audioCodec: String?,
)
