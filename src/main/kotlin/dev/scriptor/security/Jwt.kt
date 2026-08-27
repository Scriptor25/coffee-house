package dev.scriptor.security

import dev.scriptor.get
import dev.scriptor.jsonObject
import dev.scriptor.jsonOf
import dev.scriptor.parseJson
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.time.Instant

private val BASE64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

private fun String.toBase64() = BASE64.encode(this.encodeToByteArray())

data class JwtHeader(
    val typ: String = "JWT",
    val alg: String = "none",
) {
    companion object {
        fun decode(source: String): JwtHeader {
            val text = BASE64.decode(source).decodeToString()
            val node = parseJson(text)

            return JwtHeader(
                node["typ"].get(),
                node["alg"].get(),
            )
        }
    }

    fun toJson() = jsonObject {
        this["typ"] = jsonOf(typ)
        this["alg"] = jsonOf(alg)
    }

    override fun toString() = toJson().toString().toBase64()
}

data class JwtPayload(
    val iss: String? = null,
    val sub: String? = null,
    val aud: String? = null,
    val exp: Instant? = null,
    val nbf: Instant? = null,
    val iat: Instant? = null,
    val jti: String? = null,

    val custom: Map<String, String> = emptyMap(),
) {
    companion object {
        fun decode(source: String): JwtPayload {
            val text = BASE64.decode(source).decodeToString()
            val node = parseJson(text)

            var iss: String? = null
            var sub: String? = null
            var aud: String? = null
            var exp: Long? = null
            var nbf: Long? = null
            var iat: Long? = null
            var jti: String? = null

            val custom = mutableMapOf<String, String>()

            for ((k, v) in node.entries) {
                when (k) {
                    "iss" -> iss = v.get()
                    "sub" -> sub = v.get()
                    "aud" -> aud = v.get()
                    "exp" -> exp = v.get<Number>().toLong()
                    "nbf" -> nbf = v.get<Number>().toLong()
                    "iat" -> iat = v.get<Number>().toLong()
                    "jti" -> jti = v.get()

                    else -> custom[k] = v.get()
                }
            }

            return JwtPayload(
                iss,
                sub,
                aud,
                if (exp != null) Instant.fromEpochSeconds(exp) else null,
                if (nbf != null) Instant.fromEpochSeconds(nbf) else null,
                if (iat != null) Instant.fromEpochSeconds(iat) else null,
                jti,
                custom,
            )
        }
    }

    fun toJson() = jsonObject {
        if (iss != null) this["iss"] = jsonOf(iss)
        if (sub != null) this["sub"] = jsonOf(sub)
        if (aud != null) this["aud"] = jsonOf(aud)
        if (exp != null) this["exp"] = jsonOf(exp.epochSeconds)
        if (nbf != null) this["nbf"] = jsonOf(nbf.epochSeconds)
        if (iat != null) this["iat"] = jsonOf(iat.epochSeconds)
        if (jti != null) this["jti"] = jsonOf(jti)

        for ((k, v) in custom) this[k] = jsonOf(v)
    }

    override fun toString() = toJson().toString().toBase64()
}

data class Jwt(
    val header: JwtHeader,
    val payload: JwtPayload,
    val signature: ByteArray? = null,
) {
    companion object {
        fun encode(header: JwtHeader, payload: JwtPayload, secret: String?): Jwt {
            return Jwt(
                header,
                payload,
                if (secret != null)
                    sign(
                        header.alg,
                        "$header.$payload",
                        secret,
                    )
                else null,
            )
        }

        fun decode(token: String): Jwt? {
            val segments = token.split(".", limit = 3)

            return when (segments.size) {
                2 -> {
                    val (header, payload) = segments

                    Jwt(
                        JwtHeader.decode(header),
                        JwtPayload.decode(payload),
                    )
                }

                3 -> {
                    val (header, payload, signature) = segments

                    Jwt(
                        JwtHeader.decode(header),
                        JwtPayload.decode(payload),
                        BASE64.decode(signature),
                    )
                }

                else -> null
            }
        }
    }

    fun verify(secret: String): Boolean {
        if (signature == null) {
            return false
        }

        val sig = sign(
            header.alg,
            "$header.$payload",
            secret,
        )

        var result = true
        for (i in sig.indices) {
            result = result && (i < signature.size) && (sig[i] == signature[i])
        }

        return result
    }

    override fun toString(): String =
        if (signature != null)
            "$header.$payload.${BASE64.encode(signature)}"
        else
            "$header.$payload"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Jwt) return false

        if (header != other.header) return false
        if (payload != other.payload) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + payload.hashCode()
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        return result
    }
}

private fun sign(alg: String, message: String, secret: String): ByteArray {
    val algorithm = when (alg) {
        "HS256" -> "HmacSHA256"
        "HS384" -> "HmacSHA384"
        "HS512" -> "HmacSHA512"

        else -> error("unsupported algorithm '$alg'")
    }

    val secretKey = SecretKeySpec(secret.encodeToByteArray(), algorithm)
    val mac = Mac.getInstance(algorithm)

    mac.init(secretKey)

    return mac.doFinal(message.encodeToByteArray())
}
