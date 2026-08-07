package dev.scriptor.context

import dev.scriptor.model.Session
import dev.scriptor.model.SessionTable
import dev.scriptor.server.annotation.Context
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import kotlin.time.Clock.System.now
import kotlin.time.Instant

@Context
class AuthContext {

    fun auth(
        token: String,
        now: Instant = now(),
    ): Session? {
        return Session
            .find { (SessionTable.token eq token) and (SessionTable.expiresAt greaterEq now) }
            .limit(1)
            .singleOrNull()
    }
}
