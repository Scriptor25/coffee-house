package dev.scriptor.converter

import dev.scriptor.model.RangeHeader
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringRangeConverter : Converter<String, RangeHeader> {

    context(provider: Provider)
    override fun convert(value: String): RangeHeader {

        val range = value
            .substringAfter("bytes=")
            .split("-", limit = 2)
            .filter { it.isNotBlank() }

        val begin = range[0].toLong()
        val end =
            if (range.size == 1) null
            else range[1].toLong()

        return RangeHeader(begin, end)
    }
}