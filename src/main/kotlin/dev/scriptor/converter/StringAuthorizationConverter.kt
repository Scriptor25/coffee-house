package dev.scriptor.converter

import dev.scriptor.model.Authorization
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringAuthorizationConverter : Converter<String, Authorization> {

    context(provider: Provider)
    override fun convert(value: String): Authorization {
        val (scheme, credentials) = value.split("\\s+".toRegex(), limit = 2)
        return Authorization(scheme, credentials)
    }
}
