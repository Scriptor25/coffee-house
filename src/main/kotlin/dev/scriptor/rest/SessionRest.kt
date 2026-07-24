package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.model.Session
import dev.scriptor.server.annotation.Endpoint
import dev.scriptor.server.annotation.Inject
import dev.scriptor.server.annotation.PathParameter
import dev.scriptor.server.annotation.Resource
import dev.scriptor.server.http.HTTPMethod
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Endpoint("/session")
class SessionRest {

    @Inject("sessions")
    lateinit var sessions: SessionContext

    @Resource("/", method = HTTPMethod.POST, result = "application/json")
    @OptIn(ExperimentalUuidApi::class)
    fun createSession(): Session {
        val id = Uuid.generateV7()
        return sessions.create(id)
    }

    @Resource("/[id]", method = HTTPMethod.POST, result = "application/json")
    fun createSession(@PathParameter("id") id: Uuid): Session {
        return sessions.create(id)
    }

    @Resource("/[id]", method = HTTPMethod.DELETE, result = "application/json")
    fun deleteSession(@PathParameter("id") id: Uuid): Session? {
        return sessions.delete(id)
    }

    @Resource("/[id]", method = HTTPMethod.GET, result = "application/json")
    fun getSession(@PathParameter("id") id: Uuid): Session? {
        return sessions[id]
    }
}
