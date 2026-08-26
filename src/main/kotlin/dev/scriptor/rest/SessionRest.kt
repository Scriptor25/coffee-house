package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.get
import dev.scriptor.model.user.User
import dev.scriptor.model.user.UserRole
import dev.scriptor.model.user.UserTable
import dev.scriptor.security.Jwt
import dev.scriptor.security.JwtHeader
import dev.scriptor.security.JwtPayload
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.Body
import dev.scriptor.server.annotation.Controller
import dev.scriptor.server.annotation.Post
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration.ofHours
import kotlin.time.Clock
import kotlin.time.toKotlinDuration

@Controller("/session")
class SessionRest {

    @Post("/", "application/json", "application/json")
    context(provider: Provider, database: Database)
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

        val role = user?.role ?: UserRole.ADMIN

        val createdAt = Clock.System.now()
        val expiresAt = createdAt + ofHours(24).toKotlinDuration()

        val jwt = Jwt.encode(
            JwtHeader(
                alg = "HS256",
            ),
            JwtPayload(
                jti = user?.id?.toString(),
                sub = user?.name,
                iat = createdAt,
                exp = expiresAt,
                aud = "coffee-house",
                iss = "dev.scriptor.coffee-house", // TODO: change to application domain

                custom = mapOf(
                    "role" to role.toString().lowercase(),
                ),
            ),
            "hello-world-secret", // TODO: change to something more secure
        )

        return jwt
    }
}
