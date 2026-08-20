package dev.scriptor.converter

import dev.scriptor.model.user.UserRole
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter

class UserRoleStringConverter : Converter<UserRole, String> {

    context(provider: Provider)
    override fun convert(value: UserRole): String = value.name
}
