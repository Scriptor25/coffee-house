package dev.scriptor.converter

import dev.scriptor.server.Provider
import dev.scriptor.server.RangeReadableByteChannel
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.MessageBody
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel

class BodyStringConverter : Converter<MessageBody, String> {

    context(provider: Provider)
    override fun convert(value: MessageBody): String {
        val size = when (val c = value.channel) {
            is RangeReadableByteChannel -> c.remaining.toInt()
            is SeekableByteChannel -> (c.size() - c.position()).toInt()
            else -> 0
        }

        val stream = ByteArrayOutputStream(size)
        val channel = Channels.newChannel(stream)
        value.write(channel)

        return stream.toString(Charsets.UTF_8)
    }
}
