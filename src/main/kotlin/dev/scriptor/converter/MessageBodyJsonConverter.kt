package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.parseJson
import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.converter.MessageBodyStringConverter
import dev.scriptor.server.http.MessageBody

class MessageBodyJsonConverter : Converter<MessageBody, JsonNode> {

    context(provider: Provider?)
    override fun convert(value: MessageBody): JsonNode {
        val text = MessageBodyStringConverter()(value)

        return parseJson(text)
    }
}
