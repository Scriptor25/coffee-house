package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import java.sql.Timestamp
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

class TimestampInstantConverter : Converter<Timestamp, Instant> {

    context(provider: Provider)
    override fun convert(value: Timestamp) = value.toInstant().toKotlinInstant()
}
