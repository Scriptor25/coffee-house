package dev.scriptor.converter

import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringAuthorizationHeaderConverter : Converter<String, AuthorizationHeader> {

    context(provider: Provider?)
    override fun convert(value: String): AuthorizationHeader = AuthorizationHeader.parse(value)
}
