package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("audio_track")
data class AudioTrack(

    @Column("media_id")
    @PrimaryKey
    @ForeignKey(Media::class, "id")
    val mediaId: Uuid,

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
) : Track
