package dev.scriptor

import java.io.InputStream

class RangeInputStream(
    private val stream: InputStream,
    private var remaining: Long = 0,
) : InputStream() {

    override fun read(): Int {
        if (remaining <= 0L) {
            return -1
        }

        --remaining

        return stream.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0L) {
            return -1
        }

        val allowed = minOf(length.toLong(), remaining).toInt()
        val count = stream.read(buffer, offset, allowed)

        if (count > 0) {
            remaining -= count
        }

        return count
    }

    override fun close() {
        stream.close()
    }
}