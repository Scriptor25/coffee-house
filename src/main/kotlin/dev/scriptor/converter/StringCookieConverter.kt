package dev.scriptor.converter

import dev.scriptor.model.CookieHeader
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringCookieConverter : Converter<String, CookieHeader> {

    context(provider: Provider)
    override fun convert(value: String): CookieHeader {
        val values = value
            .split(";")
            .map { it.trim().split("=", limit = 2) }
            .associate { it[0] to it[1] }
        return CookieHeader(values)
    }
}
