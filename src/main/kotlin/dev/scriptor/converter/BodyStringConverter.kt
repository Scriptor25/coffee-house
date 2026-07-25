package dev.scriptor.converter

import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.HTTPMessageBody
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class BodyStringConverter : Converter<HTTPMessageBody, String> {

    override fun convert(value: HTTPMessageBody): String {
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
