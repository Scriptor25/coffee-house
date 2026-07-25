package dev.scriptor.context

import dev.scriptor.model.Session
import dev.scriptor.server.annotation.Context
import dev.scriptor.server.annotation.Inject
import kotlin.time.Clock.System.now
import kotlin.time.Instant

@Context("auth")
class AuthContext {

    @Inject("sessions")
    lateinit var sessions: SessionContext

    fun auth(token: String, now: Instant = now()): Session? {
        val session = sessions.getSessionByToken(token)
            ?: return null

        if ((session.expiresAt - now).isNegative()) {
            return null
        }

        return session
    }
}
