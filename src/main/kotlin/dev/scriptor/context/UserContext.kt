package dev.scriptor.context

import dev.scriptor.EntityConnection
import dev.scriptor.eq
import dev.scriptor.get
import dev.scriptor.model.User
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context

@Context
class UserContext {

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getUserByName(name: String): User? =
        connection.get<User>("name" eq name)
}
