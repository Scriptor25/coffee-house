package dev.scriptor.model.ffmpeg

import dev.scriptor.*
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

inline fun <reified T : Any> Table.json(
    name: String,
    crossinline from: (JsonNode) -> T,
    crossinline to: (T) -> JsonNode,
): Column<T> {
    return registerColumn(name, object : ColumnType<T>() {
        override fun sqlType(): String {
            return "TEXT"
        }

        override fun valueFromDB(value: Any): T {
            return when (value) {
                is T -> value
                is String -> from(parseJson(value))
                else -> error("unexpected value of type ${value::class}")
            }
        }

        override fun notNullValueToDB(value: T): Any {
            return to(value).toString()
        }
    })
}

object ImplementationTable : IdTable<ImplementationId>("implementation") {
    override val id = implementationId("id").entityId()

    val codec = reference("codec_id", CodecTable, ReferenceOption.CASCADE).nullable()
    val direction = enumeration<ImplementationDirection>("direction")

    val frameLevelMultithreading = bool("frame_level_multithreading")
    val sliceLevelMultithreading = bool("slice_level_multithreading")
    val experimental = bool("experimental")
    val supportDrawHorizontalBand = bool("support_draw_horizontal_band")
    val supportDirectRendering = bool("support_direct_rendering")

    val kind = enumeration<ImplementationKind>("kind")

    val generalCapabilities = json<Set<String>>(
        "general_capabilities",
        from = {
            if (it !is JsonArrayNode) {
                error("invalid node")
            }

            it.map { node ->
                if (node !is JsonStringNode) {
                    error("invalid node '[N]'")
                }

                node.value
            }.toSet()
        },
        to = { jsonArray { it.forEach { entry -> add(jsonOf(entry)) } } },
    ).default(emptySet())

    val supportedSampleRates = json<Set<Long>>(
        "supported_sample_rates",
        from = {
            if (it !is JsonArrayNode) {
                error("invalid node")
            }

            it.map { node ->
                if (node !is JsonNumberNode) {
                    error("invalid node '[N]'")
                }

                node.value.toLong()
            }.toSet()
        },
        to = { jsonArray { it.forEach { entry -> add(jsonOf(entry)) } } },
    ).default(emptySet())

    val supportedSampleFormats = json<Set<String>>(
        "supported_sample_formats",
        from = {
            if (it !is JsonArrayNode) {
                error("invalid node")
            }

            it.map { node ->
                if (node !is JsonStringNode) {
                    error("invalid node '[N]'")
                }

                node.value
            }.toSet()
        },
        to = { jsonArray { it.forEach { entry -> add(jsonOf(entry)) } } },
    ).default(emptySet())

    val supportedChannelLayouts = json<Set<String>>(
        "supported_channel_layouts",
        from = {
            if (it !is JsonArrayNode) {
                error("invalid node")
            }

            it.map { node ->
                if (node !is JsonStringNode) {
                    error("invalid node '[N]'")
                }

                node.value
            }.toSet()
        },
        to = { jsonArray { it.forEach { entry -> add(jsonOf(entry)) } } },
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
