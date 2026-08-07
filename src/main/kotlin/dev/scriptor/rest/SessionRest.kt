package dev.scriptor.rest

import dev.scriptor.EntityConnection
import dev.scriptor.JsonNode
import dev.scriptor.context.SessionContext
import dev.scriptor.context.UserContext
import dev.scriptor.get
import dev.scriptor.model.Bearer
import dev.scriptor.model.Session
import dev.scriptor.model.User
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.Body
import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Header
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.http.Method.DELETE
import dev.scriptor.server.http.Method.POST
import java.security.SecureRandom
import java.time.Duration.ofMinutes
import java.util.logging.Logger
import kotlin.io.encoding.Base64
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Endpoint("/session")
class SessionRest {

    private val random = SecureRandom()

    @Resource(
        "/",
        POST,
        "application/json",
        "application/json",
    )
    @OptIn(ExperimentalUuidApi::class)
    context(
        log: Logger,
        provider: Provider,
        _: EntityConnection,
        users: UserContext,
        sessions: SessionContext,
    )
    fun createSession(@Body body: JsonNode, @Header("user-agent") agent: String?): Session {
        val username = body["username"].get<String>()
        val password = body["password"].get<String>()

        val rootUsername: String? = provider["username"]
        val rootPassword: String? = provider["password"]

        val user: User?
        if (rootUsername != null && rootPassword != null && username == rootUsername) {
            user = null

            if (password != rootPassword) {
                throw UnauthorizedSignal()
            }
        } else {
            user = users.getUserByName(username)
                ?: throw UnauthorizedSignal()

            // TODO: check password hash
        }

        val bytes = ByteArray(24) { 0 }
        random.nextBytes(bytes)

        val token = Base64.encode(bytes)

        val createdAt = now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        val id = Uuid.generateV7()
        val session = Session(
            id,
            user,
            token,
            createdAt,
            expiresAt,
            agent,
        )

        return sessions.createSession(session)
            ?: error("failed to create session")
    }

    @Resource(
        "/",
        result = "application/json",
    )
    context(
        _: Provider,
        _: EntityConnection,
        sessions: SessionContext,
    )
    fun getCurrentSession(@Header authorization: Bearer): Session {
        return sessions.getSessionByToken(authorization.token)
            ?: throw NotFoundSignal()
    }

    @Resource(
        "/",
        DELETE,
        result = "application/json",
    )
    context(
        _: Provider,
        _: EntityConnection,
        sessions: SessionContext,
    )
    fun deleteSessionById(@Header authorization: Bearer): Session {
        val session = sessions.getSessionByToken(authorization.token)
            ?: throw NotFoundSignal()
        return sessions.deleteSession(session)
            ?: error("failed to delete session")
    }
}
