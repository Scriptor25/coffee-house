package dev.scriptor.model.ffmpeg

import dev.scriptor.fromJsonNoContext
import dev.scriptor.json
import dev.scriptor.toJsonNoContext
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object ImplementationTable : IdTable<ImplementationId>("implementation") {
    override val id = implementationId("id").entityId()

    val codec = reference("codec_id", CodecTable, ReferenceOption.CASCADE).nullable().default(null)
    val direction = enumeration<ImplementationDirection>("direction")

    val frameLevelMultithreading = bool("frame_level_multithreading")
    val sliceLevelMultithreading = bool("slice_level_multithreading")
    val experimental = bool("experimental")
    val supportDrawHorizontalBand = bool("support_draw_horizontal_band")
    val supportDirectRendering = bool("support_direct_rendering")

    val kind = enumeration<ImplementationKind>("kind")

    val generalCapabilities = json<Set<String>>(
        "general_capabilities",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    ).default(emptySet())

    val supportedSampleRates = json<Set<Long>>(
        "supported_sample_rates",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    ).default(emptySet())

    val supportedSampleFormats = json<Set<String>>(
        "supported_sample_formats",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    ).default(emptySet())

    val supportedChannelLayouts = json<Set<String>>(
        "supported_channel_layouts",
        from = { it.fromJsonNoContext() },
        to = { it.toJsonNoContext() },
    ).default(emptySet())
}

class Implementation(id: EntityID<ImplementationId>) : Entity<ImplementationId>(id) {
    companion object : EntityClass<ImplementationId, Implementation>(ImplementationTable)

    var codec by Codec optionalReferencedOn ImplementationTable.codec
    var direction by ImplementationTable.direction

    var frameLevelMultithreading by ImplementationTable.frameLevelMultithreading
    var sliceLevelMultithreading by ImplementationTable.sliceLevelMultithreading
    var experimental by ImplementationTable.experimental
    var supportDrawHorizontalBand by ImplementationTable.supportDrawHorizontalBand
    var supportDirectRendering by ImplementationTable.supportDirectRendering

    var kind by ImplementationTable.kind

    var generalCapabilities by ImplementationTable.generalCapabilities

    var supportedSampleRates by ImplementationTable.supportedSampleRates
    var supportedSampleFormats by ImplementationTable.supportedSampleFormats
    var supportedChannelLayouts by ImplementationTable.supportedChannelLayouts

    val devices by Device via ImplementationDeviceTable
    val formats by Format via ImplementationFormatTable

    override fun toString(): String {
        return "Implementation(id=$id, codec=${codec?.id}, direction=$direction, frameLevelMultithreading=$frameLevelMultithreading, sliceLevelMultithreading=$sliceLevelMultithreading, experimental=$experimental, supportDrawHorizontalBand=$supportDrawHorizontalBand, supportDirectRendering=$supportDirectRendering, kind=$kind, generalCapabilities=$generalCapabilities, supportedSampleRates=$supportedSampleRates, supportedSampleFormats=$supportedSampleFormats, supportedChannelLayouts=$supportedChannelLayouts)"
    }
}

object ImplementationDeviceTable : Table("implementation_device") {
    val implementation = reference("implementation", ImplementationTable, ReferenceOption.CASCADE)
    val device = reference("device", DeviceTable, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(implementation, device)
}

object ImplementationFormatTable : Table("implementation_format") {
    val implementation = reference("implementation", ImplementationTable, ReferenceOption.CASCADE)
    val format = reference("format", FormatTable, ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(implementation, format)
}
