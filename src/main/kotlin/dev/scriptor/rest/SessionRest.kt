package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.get
import dev.scriptor.model.Authorization
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserTable
import dev.scriptor.security.Jwt
import dev.scriptor.security.JwtHeader
import dev.scriptor.security.JwtPayload
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration.ofMinutes
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
    fun createSession(@Body body: JsonNode): Jwt {
        val username = body["username"].get<String>()
        val password = body["password"].get<String>()

        val rootUsername: String? = provider.getNamedT("username")
        val rootPassword: String? = provider.getNamedT("password")

        val user: User?
        if (rootUsername != null && rootPassword != null && username == rootUsername) {
            user = null

            if (password != rootPassword) {
                throw UnauthorizedSignal()
            }
        } else {
            user = transaction(database) {
                User
                    .find { UserTable.name eq username }
                    .limit(1)
                    .firstOrNull()
            } ?: throw UnauthorizedSignal()

            // TODO: generate password hash
            if (password != user.hash) {
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
        database: Database,
        auth: AuthContext,
    )
    fun renewSession(@Header authorization: Authorization): Jwt {

        val instant = Clock.System.now()

        val session = auth.auth(authorization, instant)
            ?: throw UnauthorizedSignal()

        val jwt = session.jwt

        val expiresAt = instant + ofMinutes(60).toKotlinDuration()

        return Jwt.encode(
            jwt.header,
            jwt.payload.copy(
                iat = instant,
                exp = expiresAt,
            ),
            "hello-world-secret", // TODO: change to something more secure
        )
    }
}
