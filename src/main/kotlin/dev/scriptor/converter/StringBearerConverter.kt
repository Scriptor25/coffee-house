package dev.scriptor.converter

import dev.scriptor.model.Bearer
import dev.scriptor.server.converter.Converter

class StringBearerConverter : Converter<String, Bearer> {

    override fun convert(value: String): Bearer {
        val token = value.substringAfter("Bearer ")
        if (token == value) {
            throw IllegalArgumentException("no bearer token provided")
        }
        return Bearer(token)
    }
}
