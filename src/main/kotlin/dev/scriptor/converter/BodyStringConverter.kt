package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.MessageBody
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class BodyStringConverter : Converter<MessageBody, String> {

    context(provider: Provider)
    override fun convert(value: MessageBody): String {
        val stream = ByteArrayOutputStream(
            if (value.count >= 0)
                value.count.toInt()
            else 0,
        )
        val channel = Channels.newChannel(stream)
        value.write(channel)

        return stream.toString(Charsets.UTF_8)
    }
}
