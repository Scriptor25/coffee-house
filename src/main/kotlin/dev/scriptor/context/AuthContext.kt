package dev.scriptor.context

import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.Session
import dev.scriptor.model.user.User
import dev.scriptor.security.Jwt
import dev.scriptor.server.jvm.annotation.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Logger
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Context
class AuthContext {

    context(
        log: Logger,
        database: Database,
    )
    fun auth(
        token: String?,
        instant: Instant = Clock.System.now(),
    ): Session? {
        if (token == null) {
            return null
        }

        val jwt: Jwt
        try {
            jwt = Jwt.decode(token)
                ?: return null
        } catch (e: Throwable) {
            log.warning(e.stackTraceToString())
            return null
        }

        // TODO: change to something more secure
        if (!jwt.verify("hello-world-secret")) {
            return null
        }

        val maxAge =
            when (val exp = jwt.payload.exp) {
                null -> null
                else -> {
                    val delta = exp - instant

                    if (delta.isNegative()) {
                        return null
                    }

                    delta.inWholeSeconds
                }
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

    context(
        _: Logger,
        _: Database,
    )
    fun auth(
        authorization: AuthorizationHeader?,
        cookie: CookieHeader,
        instant: Instant = Clock.System.now(),
    ): Session? {
        val token = when (val token = cookie["token"]) {
            null -> when {
                authorization == null -> {
                    return null
                }

                authorization.scheme != "Bearer" -> {
                    return null
                }

                else -> authorization.credentials
            }

            else -> token
        }

        return auth(token, instant)
    }
}
