package dev.scriptor.context

import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserTable
import dev.scriptor.security.Jwt
import dev.scriptor.security.JwtHeader
import dev.scriptor.security.JwtPayload
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.Context
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration.ofMinutes
import kotlin.time.Clock
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

@Context
class SessionContext {

    private fun generateToken(id: Uuid): Jwt {

        val createdAt = Clock.System.now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        return Jwt.encode(
            JwtHeader(
                alg = "HS256",
            ),
            JwtPayload(
                sub = id.toHexDashString(),
                iat = createdAt,
                exp = expiresAt,
                aud = "coffee-house",
                iss = "dev.scriptor.coffee-house", // TODO: change to application domain
            ),
            "hello-world-secret", // TODO: change to something more secure
        )
    }

    context(
        provider: Provider,
        database: Database,
    )
    fun createSession(username: String, password: String): Jwt {
        val rootUsername: String? = provider.getT("username")
        val rootPassword: String? = provider.getT("password")

        val id: Uuid
        if (rootUsername != null && rootPassword != null && username == rootUsername) {
            id = Uuid.NIL

            if (password != rootPassword) {
                throw UnauthorizedSignal()
            }
        } else {
            val user = transaction(database) {
                User
                    .find { UserTable.name eq username }
                    .firstOrNull()
            } ?: throw UnauthorizedSignal()

            id = user.id.value

            // TODO: generate password hash
            if (password != user.hash) {
                throw UnauthorizedSignal()
            }
        }

        return generateToken(id)
    }

    fun renewSession(principal: Principal): Jwt {
        return generateToken(principal.id)
    }
}
