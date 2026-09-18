package dev.scriptor.rest

import dev.scriptor.context.AuthContext
import dev.scriptor.context.SessionContext
import dev.scriptor.model.AuthorizationHeader
import dev.scriptor.model.CookieHeader
import dev.scriptor.model.CreateSessionBody
import dev.scriptor.security.Jwt
import dev.scriptor.server.Provider
import dev.scriptor.server.jvm.annotation.*
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.logging.Logger

@Suppress("unused")
@Controller("/resource/session")
class SessionRest {

    @Post("/", "application/json", "text/plain")
    context(
        _: Provider,
        _: Database,
        sessions: SessionContext,
    )
    fun createSession(
        @Body body: CreateSessionBody,
    ): Jwt {
        return sessions.createSession(body.username, body.password)
    }

    @Get("/renew", "text/plain")
    context(
        _: Logger,
        _: Database,
        _: AuthContext,
        sessions: SessionContext,
    )
    fun renewSession(
        @Header authorization: AuthorizationHeader? = null,
        @Header cookie: CookieHeader = CookieHeader(),
    ): Jwt {
        return sessions.renewSession(authorization, cookie)
    }
}
