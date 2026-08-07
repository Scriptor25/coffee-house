package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import java.sql.Timestamp
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class InstantTimestampConverter : Converter<Instant, Timestamp> {

    context(provider: Provider)
    override fun convert(value: Instant): Timestamp = Timestamp.from(value.toJavaInstant())
}
