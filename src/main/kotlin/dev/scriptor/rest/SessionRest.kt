package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.context.UserContext
import dev.scriptor.model.Session
import dev.scriptor.server.BadRequestSignal
import dev.scriptor.server.NotFoundSignal
import dev.scriptor.server.annotation.*
import dev.scriptor.server.http.HTTPMethod
import org.json.JSONObject
import java.time.Duration.ofMinutes
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

        val user = users.getUserByName(username)
            ?: throw BadRequestSignal(content = "invalid username '$username'")

        // TODO: check password hash

        val token = "" // TODO: generate token

        val createdAt = now()
        val expiresAt = createdAt + ofMinutes(60).toKotlinDuration()

        val id = Uuid.generateV7()
        val session = Session(
            id,
            user.id,
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
            ?: throw NotFoundSignal(content = "no session for id $id")
    }

    @Resource("/[id]", method = HTTPMethod.DELETE, result = "application/json")
    fun deleteSessionById(@PathParameter("id") id: Uuid): Session {
        val session = sessions.getSessionById(id)
            ?: throw NotFoundSignal(content = "no session for id $id")
        return sessions.deleteSession(session)
    }
}
