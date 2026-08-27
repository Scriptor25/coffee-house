package dev.scriptor.context

import dev.scriptor.model.Session
import dev.scriptor.model.user.UserRole
import dev.scriptor.security.Jwt
import dev.scriptor.server.annotation.Context
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Context
class AuthContext {

    fun auth(
        token: String,
        instant: Instant = Clock.System.now(),
    ): Session? {
        val jwt: Jwt = Jwt.decode(token)
            ?: return null

        // TODO: change to something more secure
        if (!jwt.verify("hello-world-secret")) {
            return null
        }

        if (jwt.payload.exp != null && jwt.payload.exp < instant) {
            return null
        }

        val id =
            when (val sub = jwt.payload.sub) {
                null -> null
                else -> Uuid.parseHexDash(sub)
            }

        val role =
            when (val role = jwt.payload.custom["role"]) {
                null -> UserRole.ADMIN
                else -> UserRole.valueOf(role.uppercase())
            }

        return Session(jwt, id, role)
    }
}
