package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table

@Table("audio_track")
data class AudioTrack(

    @Column("media_id")
    @PrimaryKey
    @ForeignKey
    val media: Media,

    @Column
    @PrimaryKey
    override val index: Int,

    @Column
    override val codec: String,

    @Column("bit_rate")
    val bitRate: Long,

    @Column("sample_rate")
    val sampleRate: Long,

    @Column
    val channels: Int,

    @Column
    override val language: String?,

    @Column
    override val title: String?,

    @Column
    override val default: Boolean,
) : Track {

    override val type: String
        get() = "a"
}
