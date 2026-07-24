package dev.scriptor.context

import dev.scriptor.model.Session
import kotlin.time.Clock.System.now
import kotlin.uuid.Uuid

class SessionContext {

    private val map: MutableMap<Uuid, Session> = HashMap()

    fun timeout(): Array<Session> {
        val now = now()

        val remove = mutableSetOf<Uuid>()
        for ((key, value) in map) {
            if (now.minus(value.access).inWholeMinutes > 60L) {
                remove.add(key)
            }
        }

        val sessions = mutableListOf<Session>()
        for (key in remove) {
            sessions.add(map.remove(key)!!)
        }

        return sessions.toTypedArray()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun create(id: Uuid): Session {
        val session = map.getOrPutIfMissing(id) { Session(id) }

        session.access = now()

        return session
    }

    fun delete(id: Uuid): Session? {
        return map.remove(id)
    }

    operator fun contains(id: Uuid): Boolean = id in map

    operator fun get(id: Uuid): Session? = map[id]
}
