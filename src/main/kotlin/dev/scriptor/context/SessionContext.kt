package dev.scriptor.context

import dev.scriptor.*
import dev.scriptor.model.Session
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import kotlin.time.Clock.System.now
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Context
class SessionContext {

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun createSession(session: Session): Session? =
        connection.create<Session>(session)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun updateSession(session: Session): Session? =
        connection.update<Session>(session)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun deleteSession(session: Session): Session? =
        connection.delete<Session>(session)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getSessionById(id: Uuid): Session? =
        connection.get<Session>(id)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getSessionByToken(token: String): Session? =
        connection.get<Session>("token" eq token)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun getExpiredSessions(now: Instant = now()): List<Session> =
        connection.getAll<Session>("expires_at" le now)

    context(
        _: Provider,
        connection: EntityConnection,
    )
    fun deleteExpiredSessions(now: Instant = now()): List<Session> =
        connection.deleteAll<Session>("expires_at" le now)
}
