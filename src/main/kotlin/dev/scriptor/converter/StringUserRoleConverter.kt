package dev.scriptor.converter

import dev.scriptor.model.UserRole
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class StringUserRoleConverter : Converter<String, UserRole> {

    context(provider: Provider)
    override fun convert(value: String): UserRole = UserRole.valueOf(value)
}
