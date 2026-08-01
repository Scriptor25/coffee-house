package dev.scriptor.converter

import dev.scriptor.model.Cookie
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringCookieConverter : Converter<String, Cookie> {

    context(provider: Provider)
    override fun convert(value: String): Cookie {
        val values = value
            .split(";")
            .map { it.trim().split("=", limit = 2) }
            .associate { it[0] to it[1] }
        return Cookie(values)
    }
}
