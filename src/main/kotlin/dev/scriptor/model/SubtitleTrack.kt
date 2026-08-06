package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("subtitle_track")
data class SubtitleTrack(

    @Column
    override val id: Uuid,

    @Column("media_id")
    val media: Media,

    @Column
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
) : Track
