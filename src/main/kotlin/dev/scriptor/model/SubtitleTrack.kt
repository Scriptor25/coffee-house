package dev.scriptor.model

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import kotlin.uuid.Uuid

@Table("subtitle_track")
data class SubtitleTrack(

    @Column("media_id")
    @PrimaryKey
    @ForeignKey(Media::class, "id")
    val mediaId: Uuid,

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
) : Track
