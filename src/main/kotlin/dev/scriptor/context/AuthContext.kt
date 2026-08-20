package dev.scriptor.context

import dev.scriptor.model.user.Session
import dev.scriptor.model.user.SessionTable
import dev.scriptor.server.annotation.Context
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock.System.now
import kotlin.time.Instant

@Context
class AuthContext {

    context(database: Database)
    fun auth(
        token: String,
        instant: Instant = now(),
    ): Session? = transaction(database) {
        Session
            .find { (SessionTable.token eq token) and (SessionTable.expiresAt greaterEq instant) }
            .limit(1)
            .singleOrNull()
    }
}
