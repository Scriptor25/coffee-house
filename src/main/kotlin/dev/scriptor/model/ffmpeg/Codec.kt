package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object CodecTable : IdTable<CodecId>("codec") {
    override val id = codecId("id").entityId()

    val type = enumeration<CodecType>("type")

    val supportsDecoding = bool("supports_decoding")
    val supportsEncoding = bool("supports_encoding")

    val intraFrameOnly = bool("intra_frame_only")
    val lossyCompression = bool("lossy_compression")
    val losslessCompression = bool("lossless_compression")
}

class Codec(id: EntityID<CodecId>) : Entity<CodecId>(id) {
    companion object : EntityClass<CodecId, Codec>(CodecTable)

    var type by CodecTable.type

    var supportsDecoding by CodecTable.supportsDecoding
    var supportsEncoding by CodecTable.supportsEncoding

    var intraFrameOnly by CodecTable.intraFrameOnly
    var lossyCompression by CodecTable.lossyCompression
    var losslessCompression by CodecTable.losslessCompression

    val implementations by Implementation optionalReferrersOn ImplementationTable.codec

    override fun toString(): String {
        return "Codec(id=$id, type=$type, supportsDecoding=$supportsDecoding, supportsEncoding=$supportsEncoding, intraFrameOnly=$intraFrameOnly, lossyCompression=$lossyCompression, losslessCompression=$losslessCompression)"
    }
}
