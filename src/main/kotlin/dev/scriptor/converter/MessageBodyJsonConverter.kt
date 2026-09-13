package dev.scriptor.converter

import dev.scriptor.JsonNode
import dev.scriptor.parseJson
import dev.scriptor.server.Provider
import dev.scriptor.server.RangeReadableByteChannel
import dev.scriptor.server.converter.Converter
import dev.scriptor.server.http.MessageBody
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

class MessageBodyJsonConverter : Converter<MessageBody, JsonNode> {

    context(provider: Provider)
    override fun convert(value: MessageBody): JsonNode {
        val bytes = when (val c = value.channel) {
            is RangeReadableByteChannel -> {
                val count = c.remaining.toInt()

                val array = ByteArray(count)
                val buffer = ByteBuffer.wrap(array)

                while (buffer.hasRemaining()) {
                    c.read(buffer)
                }

                buffer.flip()
                array
            }

            is SeekableByteChannel -> {
                val count = (c.size() - c.position()).toInt()

                val array = ByteArray(count)
                val buffer = ByteBuffer.wrap(array)

                while (buffer.hasRemaining()) {
                    c.read(buffer)
                }

                buffer.flip()
                array
            }

            else -> {
                val stream = ByteArrayOutputStream()

                val array = ByteArray(8192)
                val chunk = ByteBuffer.wrap(array)

                while (true) {
                    chunk.clear()
                    val count = value.channel.read(chunk)
                    if (count < 0) break
                    if (count == 0) continue

                    chunk.flip()
                    stream.write(array, 0, count)
                }

                stream.toByteArray()
            }
        }

        val text = bytes.decodeToString()

        return parseJson(text)
    }
}