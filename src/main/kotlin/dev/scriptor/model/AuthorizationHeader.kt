package dev.scriptor.model

data class AuthorizationHeader(
    val scheme: String,
    val credentials: String,
)
