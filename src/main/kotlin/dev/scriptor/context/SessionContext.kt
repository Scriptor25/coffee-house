package dev.scriptor.context

import dev.scriptor.*
import dev.scriptor.model.Session
import dev.scriptor.server.annotation.Context
import java.sql.Connection
import kotlin.uuid.Uuid

@Context
class SessionContext {

    context(connection: Connection)
    fun createSession(session: Session): Session {
        SQL(connection)
            .insert<Session>(session)
            .execute()
        return session
    }

    context(connection: Connection)
    fun deleteSession(session: Session): Session {
        SQL(connection)
            .delete<Session>()
            .where(Session::id eq session.id)
            .execute()
        return session
    }

    context(connection: Connection)
    fun getSessionById(id: Uuid): Session? = SQL(connection)
        .select<Session>()
        .where(Session::id eq id)
        .limit(1)
        .query<Session>()
        .firstOrNull()

    context(connection: Connection)
    fun getSessionByToken(token: String): Session? = SQL(connection)
        .select<Session>()
        .where(Session::token eq token)
        .limit(1)
        .query<Session>()
        .firstOrNull()
}
