package dev.scriptor.context

import dev.scriptor.model.Playback
import dev.scriptor.server.jvm.annotation.Context
import java.security.SecureRandom
import java.time.Duration.ofHours
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.toKotlinDuration
import kotlin.uuid.Uuid

@Context
class PlaybackContext {

    private val random = SecureRandom()
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    private val map = mutableMapOf<String, Playback>()

    fun createPlayback(
        userId: Uuid?,
        name: String,
        items: List<Uuid>,
    ): String {
        val createdAt = Clock.System.now()
        val expiresAt = createdAt + ofHours(24).toKotlinDuration()

        while (true) {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)

            val token = base64.encode(bytes)
            if (token in map) continue

            map[token] = Playback(
                userId,
                name,
                items,
                createdAt,
                expiresAt,
            )

            return token
        }
    }

    fun getPlayback(token: String): Playback? {
        val playback = map[token]
            ?: return null

        val instant = Clock.System.now()

        val delta = playback.expiresAt - instant
        if (delta.isNegative()) {
            map.remove(token)
            return null
        }

        return playback
    }

    fun deletePlayback(token: String): Playback? {
        return map.remove(token)
    }

    fun deleteExpiredPlaybacks() {
        val instant = Clock.System.now()

        val expired = mutableListOf<String>()
        for ((key, value) in map) {
            val delta = value.expiresAt - instant
            if (delta.isNegative()) {
                expired.add(key)
            }
        }

        for (key in expired) {
            map.remove(key)
        }
    }
}

