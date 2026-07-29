package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.context.UserContext
import dev.scriptor.model.Session
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.HTTPMethod
import org.json.JSONObject
import java.security.SecureRandom
import java.time.Duration.ofMinutes
import kotlin.io.encoding.Base64
import kotlin.time.Clock.System.now
import kotlin.time.toKotlinDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Endpoint("/session")
class SessionRest {

    @Inject("users")
    lateinit var users: UserContext

    @Inject("sessions")
    lateinit var sessions: SessionContext

    @Inject("username")
    var rootUsername: String? = null

    @Inject("password")
    var rootPassword: String? = null

    private val random = SecureRandom()

    @Resource(
        path = "/",
        method = HTTPMethod.POST,
        accept = "application/json",
        result = "application/json",
    )
    @OptIn(ExperimentalUuidApi::class)
    fun createSession(@Body body: JSONObject, @Header("user-agent") agent: String?): Session {
        val username = body.getString("username")
        val password = body.getString("password")

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
            null,
            agent,
            0L,
            0L,
        )

        return sessions.createSession(session)
    }

    @Resource("/[id]", method = HTTPMethod.GET, result = "application/json")
    fun getSessionById(@PathParameter("id") id: Uuid): Session {
        return sessions.getSessionById(id)
            ?: throw NotFoundSignal()
    }

    @Resource("/[id]", method = HTTPMethod.DELETE, result = "application/json")
    fun deleteSessionById(@PathParameter("id") id: Uuid): Session {
        val session = sessions.getSessionById(id)
            ?: throw NotFoundSignal()
        return sessions.deleteSession(session)
    }
}
