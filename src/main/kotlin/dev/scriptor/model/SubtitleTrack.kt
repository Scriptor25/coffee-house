package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table

@Table("subtitle_track")
data class SubtitleTrack(

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
    override val language: String?,

    @Column
    override val title: String?,

    @Column
    override val default: Boolean,

    @Column
    val forced: Boolean,
) : Track {

    override val type: String
        get() = "s"
}
