package dev.scriptor.model.ffmpeg

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object ImplementationCapabilitiesTable : IdTable<ImplementationId>("implementation_capabilities") {
    override val id = implementationId("id").entityId()

    val codec = reference("codec_id", CodecCapabilitiesTable, ReferenceOption.CASCADE).nullable()
    val direction = enumeration<CodecDirection>("direction")

    val frameLevelMultithreading = bool("frame_level_multithreading")
    val sliceLevelMultithreading = bool("slice_level_multithreading")
    val experimental = bool("experimental")
    val supportDrawHorizontalBand = bool("support_draw_horizontal_band")
    val supportDirectRendering = bool("support_direct_rendering")

    val kind = enumeration<ImplementationKind>("kind")

    val generalCapabilities = text("general_capabilities").default("")

    val supportedPixelFormats = text("supported_pixel_formats").default("")

    val supportedSampleRates = text("supported_sample_rates").default("")
    val supportedSampleFormats = text("supported_sample_formats").default("")
    val supportedChannelLayouts = text("supported_channel_layouts").default("")
}

class ImplementationCapabilities(id: EntityID<ImplementationId>) : Entity<ImplementationId>(id) {
    companion object : EntityClass<ImplementationId, ImplementationCapabilities>(ImplementationCapabilitiesTable)

    var codec by CodecCapabilities optionalReferencedOn ImplementationCapabilitiesTable.codec
    var direction by ImplementationCapabilitiesTable.direction

    var frameLevelMultithreading by ImplementationCapabilitiesTable.frameLevelMultithreading
    var sliceLevelMultithreading by ImplementationCapabilitiesTable.sliceLevelMultithreading
    var experimental by ImplementationCapabilitiesTable.experimental
    var supportDrawHorizontalBand by ImplementationCapabilitiesTable.supportDrawHorizontalBand
    var supportDirectRendering by ImplementationCapabilitiesTable.supportDirectRendering

    var kind by ImplementationCapabilitiesTable.kind

    var generalCapabilities by ImplementationCapabilitiesTable.generalCapabilities

    var supportedPixelFormats by ImplementationCapabilitiesTable.supportedPixelFormats

    var supportedSampleRates by ImplementationCapabilitiesTable.supportedSampleRates
    var supportedSampleFormats by ImplementationCapabilitiesTable.supportedSampleFormats
    var supportedChannelLayouts by ImplementationCapabilitiesTable.supportedChannelLayouts

    val supportedHardwareDevices by DeviceCapabilities via ImplementationDeviceTable

    override fun toString(): String {
        return "ImplementationCapabilities(id=$id, codec=${codec?.id}, direction=$direction, frameLevelMultithreading=$frameLevelMultithreading, sliceLevelMultithreading=$sliceLevelMultithreading, experimental=$experimental, supportDrawHorizontalBand=$supportDrawHorizontalBand, supportDirectRendering=$supportDirectRendering, kind=$kind, generalCapabilities=$generalCapabilities, supportedPixelFormats=$supportedPixelFormats, supportedSampleRates=$supportedSampleRates, supportedSampleFormats=$supportedSampleFormats, supportedChannelLayouts=$supportedChannelLayouts, supportedHardwareDevices=$supportedHardwareDevices)"
    }
}

object ImplementationDeviceTable : Table("implementation_device") {
    val implementation = reference("implementation", ImplementationCapabilitiesTable, ReferenceOption.CASCADE)
    val device = reference("device", DeviceCapabilitiesTable, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(implementation, device)
}
