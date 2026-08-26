package dev.scriptor.converter

import dev.scriptor.security.Jwt
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class JwtStringConverter : Converter<Jwt, String> {

    context(provider: Provider)
    override fun convert(value: Jwt) = value.toString()
}
