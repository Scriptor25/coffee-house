package dev.scriptor.model

class Cookie {

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
