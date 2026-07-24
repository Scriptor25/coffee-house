package dev.scriptor.context

import dev.scriptor.model.Session
import kotlin.time.Clock.System.now
import kotlin.uuid.Uuid

class SessionContext {

    private val map: MutableMap<Uuid, Session> = HashMap()

    fun create(id: Uuid): Session {
        if (id in map) return map[id]!!

        val session = Session(id, now())

        map[id] = session

        return session
    }

    fun delete(id: Uuid): Session? {
        val session = map[id] ?: return null

        map.remove(id)

        session.open = false
        return session
    }

    operator fun contains(id: Uuid): Boolean = id in map

    operator fun get(id: Uuid): Session? = map[id]
}
