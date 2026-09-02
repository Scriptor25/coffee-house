package dev.scriptor.context

import dev.scriptor.model.Authorization
import dev.scriptor.model.Session
import dev.scriptor.model.user.User
import dev.scriptor.security.Jwt
import dev.scriptor.server.jvm.annotation.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Context
class AuthContext {

    context(database: Database)
    fun auth(
        token: String,
        instant: Instant = Clock.System.now(),
    ): Session? {
        val jwt = Jwt.decode(token)
            ?: return null

        // TODO: change to something more secure
        if (!jwt.verify("hello-world-secret")) {
            return null
        }

        var maxAge: Long? = null
        if (jwt.payload.exp != null) {
            val delta = jwt.payload.exp - instant

            if (delta.isNegative()) {
                return null
            }

            maxAge = delta.inWholeSeconds
        }

        val id =
            when (val sub = jwt.payload.sub) {
                null -> null
                else -> Uuid.parseHexDash(sub)
            }

        val user =
            if (id == null) null
            else transaction(database) { User.findById(id) }

        return Session(token, jwt, maxAge, id, user)
    }

    context(_: Database)
    fun auth(
        authorization: Authorization?,
        instant: Instant = Clock.System.now(),
    ): Session? {
        if (authorization == null) {
            return null
        }

        if (authorization.scheme != "Bearer") {
            return null
        }

        val token = authorization.credentials

        return auth(token, instant)
    }
}
