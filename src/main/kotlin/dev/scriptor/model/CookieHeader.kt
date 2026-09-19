package dev.scriptor.model

class CookieHeader {

    companion object {
        fun parse(value: String): CookieHeader {
            val values = value
                .split(";")
                .map { it.trim().split("=", limit = 2) }
                .associate { it[0] to it[1] }
            return CookieHeader(values)
        }
    }

    private val values: MutableMap<String, String>

    constructor() {
        this.values = HashMap()
    }

    constructor(values: Map<String, String>) {
        this.values = HashMap(values)
    }

    operator fun get(key: String): String? = values[key]

    operator fun set(key: String, value: String) {
        values[key] = value
    }

    operator fun contains(key: String): Boolean = key in values
}
