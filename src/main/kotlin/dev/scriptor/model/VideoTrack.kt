package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table

@Table("video_track")
data class VideoTrack(

    @Column("media_id")
    @PrimaryKey
    @ForeignKey
    val media: Media,

    @Column
    @PrimaryKey
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
) : Track {

    override val type: String
        get() = "v"
}
