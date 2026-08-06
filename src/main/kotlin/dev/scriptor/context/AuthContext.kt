package dev.scriptor.context

import dev.scriptor.EntityConnection
import dev.scriptor.model.Session
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import kotlin.time.Clock.System.now
import kotlin.time.Instant

@Context
class AuthContext {

    context(
        _: Provider,
        _: EntityConnection,
        sessions: SessionContext,
    )
    fun auth(
        token: String,
        now: Instant = now(),
    ): Session? {
        val session = sessions.getSessionByToken(token)
            ?: return null

        if ((session.expiresAt - now).isNegative()) {
            return null
        }

        return session
    }
}
