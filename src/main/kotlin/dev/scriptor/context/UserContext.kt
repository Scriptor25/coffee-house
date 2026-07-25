package dev.scriptor.context

import dev.scriptor.SQL
import dev.scriptor.eq
import dev.scriptor.query
import dev.scriptor.model.User
import dev.scriptor.selectFrom
import dev.scriptor.server.annotation.Context
import dev.scriptor.server.annotation.Inject
import java.sql.Connection

@Context("users")
class UserContext {

    @Inject("connection")
    lateinit var connection: Connection

    fun getUserByName(name: String): User? = SQL()
        .selectFrom<User>()
        .where(User::name eq name)
        .limit(1)
        .query<User>(connection)
        .firstOrNull()
}
