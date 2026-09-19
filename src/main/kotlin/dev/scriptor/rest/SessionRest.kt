package dev.scriptor.rest

import dev.scriptor.context.SessionContext
import dev.scriptor.model.CreateSessionBody
import dev.scriptor.security.Jwt
import dev.scriptor.server.Provider
import dev.scriptor.server.UnauthorizedSignal
import dev.scriptor.server.jvm.annotation.*
import dev.scriptor.server.security.Principal
import org.jetbrains.exposed.v1.jdbc.Database

@Controller("/resource/session")
class SessionRest {

    @Post("/", "application/json", "text/plain")
    context(_: Provider, _: Database, sessions: SessionContext)
    fun createSession(@Body body: CreateSessionBody): Jwt {
        return sessions.createSession(body.username, body.password)
            ?: throw UnauthorizedSignal()
    }

    @RequireAuth
    @Get("/renew", "text/plain")
    context(principal: Principal, sessions: SessionContext)
    fun renewSession(): Jwt {
        return sessions.renewSession(principal)
    }
}
