package dev.scriptor.context

import dev.scriptor.*
import dev.scriptor.model.Session
import dev.scriptor.server.Provider
import dev.scriptor.server.annotation.Context
import java.sql.Connection
import kotlin.time.Clock.System.now
import kotlin.uuid.Uuid

@Context
class SessionContext {

    context(
        _: Provider,
        connection: Connection,
    )
    fun createSession(session: Session) {
        SQL(connection)
            .insert<Session>(session)
            .execute()
    }

    context(
        _: Provider,
        connection: Connection,
    )
    fun updateSession(session: Session) {
        SQL(connection)
            .update<Session>(session, Session::id eq session.id)
            .execute()
    }

    context(
        _: Provider,
        connection: Connection,
    )
    fun deleteSession(session: Session) {
        SQL(connection)
            .delete<Session>()
            .where(Session::id eq session.id)
            .execute()
    }

    context(
        _: Provider,
        connection: Connection,
    )
    fun getSessionById(id: Uuid): Session? = SQL(connection)
        .select<Session>()
        .where(Session::id eq id)
        .limit(1)
        .query<Session>()
        .firstOrNull()

    context(
        _: Provider,
        connection: Connection,
    )
    fun getSessionByToken(token: String): Session? = SQL(connection)
        .select<Session>()
        .where(Session::token eq token)
        .limit(1)
        .query<Session>()
        .firstOrNull()

    context(
        _: Provider,
        connection: Connection,
    )
    fun getExpiredSessions(): List<Session> = SQL(connection)
        .select<Session>()
        .where(Session::expiresAt le now())
        .query<Session>()
}
