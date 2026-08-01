package dev.scriptor.context

import dev.scriptor.SQL
import dev.scriptor.eq
import dev.scriptor.model.User
import dev.scriptor.query
import dev.scriptor.select
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import java.sql.Connection

@Context
class UserContext {

    context(_: Provider, connection: Connection)
    fun getUserByName(name: String): User? = SQL(connection)
        .select<User>()
        .where(User::name eq name)
        .limit(1)
        .query<User>()
        .firstOrNull()
}
