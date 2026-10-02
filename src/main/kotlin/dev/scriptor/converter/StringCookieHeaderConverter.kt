package dev.scriptor.converter

import dev.scriptor.model.CookieHeader
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringCookieHeaderConverter : Converter<String, CookieHeader> {

    context(provider: Provider?)
    override fun convert(value: String): CookieHeader = CookieHeader.parse(value)
}
