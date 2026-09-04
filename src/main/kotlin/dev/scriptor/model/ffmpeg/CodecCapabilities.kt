package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object CodecCapabilitiesTable : IdTable<CodecId>("codec_capabilities") {
    override val id = codecId("id").entityId()

    val type = enumeration<CodecType>("type")

    val supportsDecoding = bool("supports_decoding")
    val supportsEncoding = bool("supports_encoding")

    val intraFrameOnly = bool("intra_frame_only")
    val lossyCompression = bool("lossy_compression")
    val losslessCompression = bool("lossless_compression")
}

class CodecCapabilities(id: EntityID<CodecId>) : Entity<CodecId>(id) {
    companion object : EntityClass<CodecId, CodecCapabilities>(CodecCapabilitiesTable)

    var type by CodecCapabilitiesTable.type

    var supportsDecoding by CodecCapabilitiesTable.supportsDecoding
    var supportsEncoding by CodecCapabilitiesTable.supportsEncoding

    var intraFrameOnly by CodecCapabilitiesTable.intraFrameOnly
    var lossyCompression by CodecCapabilitiesTable.lossyCompression
    var losslessCompression by CodecCapabilitiesTable.losslessCompression

    val implementations by ImplementationCapabilities optionalReferrersOn ImplementationCapabilitiesTable.codec

    override fun toString(): String {
        return "CodecCapabilities(id=$id, type=$type, supportsDecoding=$supportsDecoding, supportsEncoding=$supportsEncoding, intraFrameOnly=$intraFrameOnly, lossyCompression=$lossyCompression, losslessCompression=$losslessCompression)"
    }
}
