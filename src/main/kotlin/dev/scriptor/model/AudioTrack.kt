package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("audio_track")
data class AudioTrack(

    @Column
    override val id: Uuid,

    @Column("media_id", unique = "cnt_media_index")
    val media: Media,

    @Column(unique = "cnt_media_index")
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
) : Track
