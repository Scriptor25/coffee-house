package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.model.Authorization
import dev.scriptor.model.CreateSessionBody
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserTable
import dev.scriptor.security.Jwt
import dev.scriptor.security.JwtHeader
import dev.scriptor.security.JwtPayload
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration.ofMinutes
import java.util.logging.Logger
import kotlin.time.Clock
import kotlin.time.toKotlinDuration

@Suppress("unused")
@Controller("/session")
class SessionRest {

    @Post("/", "application/json", "text/plain")
    context(
        provider: Provider,
        database: Database,
    )
    fun createSession(
        @Body body: CreateSessionBody,
    ): Jwt {
        val rootUsername = provider.getNamedT<String>("username")
        val rootPassword = provider.getNamedT<String>("password")

        val user: User?
        if (rootUsername != null && rootPassword != null && body.username == rootUsername) {
            user = null

            if (body.password != rootPassword) {
                throw UnauthorizedSignal()
            }
        } else {
            user = transaction(database) {
                User
                    .find { UserTable.name eq body.username }
                    .firstOrNull()
            } ?: throw UnauthorizedSignal()

            // TODO: generate password hash
            if (body.password != user.hash) {
                throw UnauthorizedSignal()
            }
        }

        val createdAt = Clock.System.now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        val jwt = Jwt.encode(
            JwtHeader(
                alg = "HS256",
            ),
            JwtPayload(
                sub = user?.id?.toString(),
                iat = createdAt,
                exp = expiresAt,
                aud = "coffee-house",
                iss = "dev.scriptor.coffee-house", // TODO: change to application domain
            ),
            "hello-world-secret", // TODO: change to something more secure
        )

        return jwt
    }

    @Get("/renew", "text/plain")
    context(
        _: Logger,
        database: Database,
        auth: AuthContext,
    )
    fun renewSession(
        @Header authorization: Authorization? = null,
    ): Jwt {

        val instant = Clock.System.now()

        val session = auth.auth(authorization, instant)
            ?: throw UnauthorizedSignal()

        val jwt = session.jwt

        val expiresAt = instant + ofMinutes(60).toKotlinDuration()

        return Jwt.encode(
            jwt.header,
            jwt.payload.copy(
                iat = instant.epochSeconds,
                exp = expiresAt.epochSeconds,
            ),
            "hello-world-secret", // TODO: change to something more secure
        )
    }
}
