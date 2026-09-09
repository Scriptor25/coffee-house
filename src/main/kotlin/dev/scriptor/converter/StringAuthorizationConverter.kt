package dev.scriptor.converter

import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringAuthorizationConverter : Converter<String, AuthorizationHeader> {

    context(provider: Provider)
    override fun convert(value: String): AuthorizationHeader {
        val (scheme, credentials) = value.split("\\s+".toRegex(), limit = 2)
        return AuthorizationHeader(scheme, credentials)
    }
}
