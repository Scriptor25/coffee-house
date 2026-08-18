package dev.scriptor.rest

import dev.scriptor.JsonNode
import dev.scriptor.context.AuthContext
import dev.scriptor.get
import dev.scriptor.model.Authorization
import dev.scriptor.model.Session
import dev.scriptor.model.User
import dev.scriptor.model.UserTable
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.SecureRandom
import java.time.Duration.ofMinutes
import kotlin.io.encoding.Base64
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinDuration

@Controller("/session")
class SessionRest {

    private val random = SecureRandom()

    @Post("/", "application/json", "application/json")
    context(provider: Provider, database: Database)
    fun createSession(
        @Body body: JsonNode,
        @Header("user-agent") agent: String?,
    ): Session {
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

        val bytes = ByteArray(24) { 0 }
        random.nextBytes(bytes)

        val token = Base64.encode(bytes)

        val createdAt = now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        return transaction(database) {
            Session.new {
                this.user = user
                this.token = token
                this.createdAt = createdAt
                this.expiresAt = expiresAt
                this.agent = agent
            }
        }
    }

    @Get("/", result = "application/json")
    context(auth: AuthContext, database: Database)
    fun getCurrentSession(@Header authorization: Authorization): Session =
        auth.auth(authorization.credentials)
            ?: throw NotFoundSignal()

    @Delete("/", result = "application/json")
    context(auth: AuthContext, database: Database)
    fun deleteCurrentSession(@Header authorization: Authorization): Session {
        val session = auth.auth(authorization.credentials)
            ?: throw NotFoundSignal()

        transaction(database) { session.delete() }
        return session
    }
}
