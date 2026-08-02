package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.context.UserContext
import dev.scriptor.model.Bearer
import dev.scriptor.model.Session
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.Body
import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Header
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.http.Method.DELETE
import dev.scriptor.server.http.Method.POST
import org.json.JSONObject
import java.security.SecureRandom
import java.sql.Connection
import java.time.Duration.ofMinutes
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
        provider: Provider,
        _: Connection,
        users: UserContext,
        sessions: SessionContext,
    )
    fun createSession(@Body body: JSONObject, @Header("user-agent") agent: String?): Session {
        val username = body.getString("username")
        val password = body.getString("password")

        val rootUsername: String? = provider["username"]
        val rootPassword: String? = provider["password"]

        val userId: Uuid?
        if (rootUsername != null && rootPassword != null && username == rootUsername) {
            if (password != rootPassword) {
                throw UnauthorizedSignal()
            }

            userId = null
        } else {
            val user = users.getUserByName(username)
                ?: throw UnauthorizedSignal()

            // TODO: check password hash

            userId = user.id
        }

        val bytes = ByteArray(24) { 0 }
        random.nextBytes(bytes)

        val token = Base64.encode(bytes)

        val createdAt = now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        val id = Uuid.generateV7()
        val session = Session(
            id,
            userId,
            token,
            createdAt,
            expiresAt,
            agent,
        )

        sessions.createSession(session)
        return session
    }

    @Resource(
        "/",
        result = "application/json",
    )
    context(
        _: Provider,
        _: Connection,
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
        _: Connection,
        sessions: SessionContext,
    )
    fun deleteSessionById(@Header authorization: Bearer): Session {
        val session = sessions.getSessionByToken(authorization.token)
            ?: throw NotFoundSignal()
        sessions.deleteSession(session)
        return session
    }
}
