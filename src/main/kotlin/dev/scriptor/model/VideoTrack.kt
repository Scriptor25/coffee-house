package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("video_track")
data class VideoTrack(

    @Column
    override val id: Uuid,

    @Column("media_id", unique = "cnt_media_index")
    val media: Media,

    @Column(unique = "cnt_media_index")
    override val index: Int,

    @Column
    override val codec: String,

    @Column
    val width: Int,

    @Column
    val height: Int,

    @Column("bit_rate")
    val bitRate: Long,

    @Column("frame_rate")
    val frameRate: Double,

    @Column
    val profile: String?,

    @Column
    val level: Int?,

    @Column
    val hdr: Boolean,

    @Column
    override val language: String?,

    @Column
    override val title: String?,

    @Column
    override val default: Boolean,
) : Track
