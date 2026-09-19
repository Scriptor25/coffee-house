package dev.scriptor.model

data class AuthorizationHeader(
    val scheme: String,
    val credentials: String,
) {
    companion object {
        fun parse(value: String): AuthorizationHeader {
            val (scheme, credentials) = value.split("\\s+".toRegex(), limit = 2)
            return AuthorizationHeader(scheme, credentials)
        }
    }
}
