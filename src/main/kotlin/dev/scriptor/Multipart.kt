package dev.scriptor

data class MimeHeaders(
    val fields: Map<String, String>,
) {
    operator fun get(name: String): String? = fields[name.lowercase()]
}

data class ContentType(
    val type: String,
    val subtype: String,
    val parameters: Map<String, String>,
) {
    val mediaType: String
        get() = "$type/$subtype"

    val isMultipart: Boolean
        get() = type.equals("multipart", ignoreCase = true)

    val boundary: String?
        get() = parameters["boundary"]
}

class MimePart(
    val headers: MimeHeaders,
    val contentType: ContentType,
    val body: ByteArray,
    val children: List<MimePart> = emptyList(),
) {
    val isMultipart: Boolean
        get() = contentType.isMultipart

    override fun toString(): String {
        return "MimePart(headers=$headers, contentType=$contentType, body=ByteArray(size=${body.size}), children=$children)"
    }
}

class MultipartMessage(
    val contentType: ContentType,
    val preamble: ByteArray,
    val parts: List<MimePart>,
    val epilogue: ByteArray,
) {

    override fun toString(): String {
        return "MultipartMessage(contentType=$contentType, preamble=ByteArray(size=${preamble.size}), parts=$parts, epilogue=ByteArray(size=${epilogue.size}))"
    }
}

class MultipartParser(private val input: ByteArray) {

    private var pos = 0

    fun parse(contentTypeHeader: String): MultipartMessage {
        val contentType = parseContentType(contentTypeHeader)

        require(contentType.isMultipart) {
            "expected multipart content-type"
        }

        val boundary = contentType.boundary
            ?: error("multipart content-type has no boundary parameters")

        validateBoundary(boundary)

        return parseMultipart(contentType, boundary)
    }

    private fun parseMultipart(contentType: ContentType, boundary: String): MultipartMessage {
        val delimiter = "--$boundary".toByteArray(Charsets.US_ASCII)

        val first = findBoundary(delimiter, pos)
        if (first < 0) {
            error("multipart is referencing invalid boundary '$boundary'")
        }

        val preamble = input.copyOfRange(pos, first)

        pos = first

        val parts = mutableListOf<MimePart>()
        while (true) {
            val boundaryInfo = consumeBoundary(delimiter)

            if (boundaryInfo == Boundary.CLOSE) {
                val epilogue = input.copyOfRange(pos, input.size)

                return MultipartMessage(
                    contentType,
                    preamble,
                    parts,
                    epilogue,
                )
            }

            require(consumeCRLF()) {
                "multipart boundary is not followed by crlf"
            }

            val headers = parseHeaders()

            val bodyStart = pos

            val next = findBoundary(delimiter, pos)
            if (next < 0) {
                error("unexpected end of multipart body")
            }

            val bodyEnd = if (
                next >= 2 &&
                input[next - 2] == '\r'.code.toByte() &&
                input[next - 1] == '\n'.code.toByte()
            ) {
                next - 2
            } else {
                next
            }

            val body = input.copyOfRange(bodyStart, bodyEnd)

            val partContentType = parsePartContentType(headers, contentType)

            val children = if (partContentType.isMultipart) {
                MultipartParser(body).parse(
                    headers["content-type"]
                        ?: error("nested multipart is missing content-type header")
                ).parts
            } else {
                emptyList()
            }

            parts += MimePart(
                headers,
                contentType,
                body,
                children,
            )

            pos = next
        }
    }

    private fun parsePartContentType(headers: MimeHeaders, parent: ContentType): ContentType {
        val value = headers["content-type"]

        return when {
            value != null -> {
                parseContentType(value)
            }

            parent.subtype.equals("digest", ignoreCase = true) -> {
                ContentType("message", "rfc822", emptyMap())
            }

            else -> {
                ContentType("text", "plain", emptyMap())
            }
        }
    }

    private fun parseHeaders(): MimeHeaders {
        val unfolded = mutableListOf<String>()

        while (true) {
            val line = readLine()
                ?: error("unexpected end while reading MIME headers")

            if (line.isEmpty()) {
                break
            }

            if (line[0] == ' ' || line[0] == '\t') {
                require(unfolded.isNotEmpty()) {
                    "header continuation without preceding header"
                }

                unfolded[unfolded.lastIndex] += " ${line.trim()}"
            } else {
                unfolded += line
            }
        }

        val fields = linkedMapOf<String, String>()

        for (line in unfolded) {
            val colon = line.indexOf(':')

            require(colon > 0) {
                "malformed MIME header: $line"
            }

            val name = line
                .substring(0, colon)
                .trim()
                .lowercase()

            val value = line
                .substring(colon + 1)
                .trim()

            fields[name] = value
        }

        return MimeHeaders(fields)
    }

    private fun readLine(): String? {
        if (pos >= input.size) {
            return null
        }

        val start = pos

        while (pos + 1 < input.size) {
            if (
                input[pos] == '\r'.code.toByte() &&
                input[pos + 1] == '\n'.code.toByte()
            ) {
                val result = input
                    .copyOfRange(start, pos)
                    .toString(Charsets.ISO_8859_1)

                pos += 2
                return result
            }

            pos++
        }

        return null
    }

    private fun consumeCRLF(): Boolean {
        if (pos + 1 >= input.size) {
            return false
        }

        if (
            input[pos] == '\r'.code.toByte() &&
            input[pos + 1] == '\n'.code.toByte()
        ) {
            pos += 2
            return true
        }

        return false
    }

    private fun findBoundary(delimiter: ByteArray, start: Int): Int {
        var i = start
        while (i <= input.size - delimiter.size) {
            val atLineStart =
                i == 0 || (i >= 2 && input[i - 2] == '\r'.code.toByte() && input[i - 1] == '\n'.code.toByte())

            if (atLineStart && input.matchesAt(i, delimiter)) {
                val after = i + delimiter.size

                if (
                    after + 1 < input.size &&
                    input[after] == '-'.code.toByte() &&
                    input[after + 1] == '-'.code.toByte()
                ) {
                    return i
                }

                var p = after

                while (
                    p < input.size &&
                    (input[p] == ' '.code.toByte() || input[p] == '\t'.code.toByte())
                ) {
                    p++
                }

                if (
                    p + 1 < input.size &&
                    input[p] == '\r'.code.toByte() &&
                    input[p + 1] == '\n'.code.toByte()
                ) {
                    return i
                }
            }

            i++
        }

        return -1
    }

    private fun consumeBoundary(delimiter: ByteArray): Boundary {
        require(input.matchesAt(pos, delimiter)) {
            "expected multipart boundary"
        }

        pos += delimiter.size

        if (
            pos + 1 < input.size &&
            input[pos] == '-'.code.toByte() &&
            input[pos + 1] == '-'.code.toByte()
        ) {
            pos += 2

            while (
                pos < input.size &&
                (input[pos] == ' '.code.toByte() || input[pos] == '\t'.code.toByte())
            ) {
                pos++
            }

            if (pos < input.size) {
                require(consumeCRLF()) {
                    "closing multipart boundary is not followed by CRLF"
                }
            }

            return Boundary.CLOSE
        }

        while (
            pos < input.size &&
            (input[pos] == ' '.code.toByte() || input[pos] == '\t'.code.toByte())
        ) {
            pos++
        }

        return Boundary.OPEN
    }

    private fun validateBoundary(boundary: String) {
        require(boundary.isNotEmpty()) {
            "empty multipart boundary"
        }

        require(boundary.length <= 70) {
            "multipart boundary exceeds RFC 2046's 70 character limit"
        }

        require(
            boundary.last() != ' ' &&
                    boundary.last() != '\t'
        ) {
            "multipart boundary must not end in whitespace"
        }

        require(
            boundary.all { it.code in 33..126 }
        ) {
            "multipart boundary contains non-ASCII/control characters"
        }
    }

    private enum class Boundary {
        OPEN,
        CLOSE
    }

    private fun ByteArray.matchesAt(
        offset: Int,
        value: ByteArray
    ): Boolean {
        if (offset < 0 || offset + value.size > size) {
            return false
        }

        for (i in value.indices) {
            if (this[offset + i] != value[i]) {
                return false
            }
        }

        return true
    }

    private fun parseContentType(
        value: String
    ): ContentType {
        val parser = ParameterParser(value)

        val mediaType = parser.readToken()
            ?: error("missing content-type")

        require(parser.consume('/')) {
            "malformed content-type: expected '/'"
        }

        val subtype = parser.readToken()
            ?: error("missing content-type subtype")

        val parameters = linkedMapOf<String, String>()

        while (parser.consume(';')) {
            val name = parser.readToken()
                ?: error("missing parameter name")

            parser.skipWhitespace()

            require(parser.consume('=')) {
                "missing '=' after parameter '$name'"
            }

            val value = parser.readParameterValue()

            parameters[name.lowercase()] = value
        }

        parser.skipWhitespace()

        require(parser.atEnd()) {
            "unexpected data after content-type"
        }

        return ContentType(
            type = mediaType.lowercase(),
            subtype = subtype.lowercase(),
            parameters = parameters
        )
    }

}

private class ParameterParser(private val input: String) {
    private var pos = 0

    fun readToken(): String? {
        skipWhitespace()

        val start = pos

        while (pos < input.length && isTokenChar(input[pos])) {
            pos++
        }

        return if (pos == start) {
            null
        } else {
            input.substring(start, pos)
        }
    }

    fun readParameterValue(): String {
        skipWhitespace()

        if (pos >= input.length) {
            error("missing parameter value")
        }

        if (input[pos] == '"') {
            return readQuotedString()
        }

        val start = pos

        while (
            pos < input.length &&
            isTokenChar(input[pos])
        ) {
            pos++
        }

        require(pos > start) {
            "invalid parameter value"
        }

        return input.substring(start, pos)
    }

    private fun readQuotedString(): String {
        require(input[pos] == '"')
        pos++

        val out = StringBuilder()

        while (pos < input.length) {
            when (val c = input[pos++]) {
                '"' -> return out.toString()

                '\\' -> {
                    require(pos < input.length) {
                        "trailing escape in quoted string"
                    }

                    out.append(input[pos++])
                }

                else -> out.append(c)
            }
        }

        error("unterminated quoted string")
    }

    fun consume(c: Char): Boolean {
        skipWhitespace()

        if (pos < input.length && input[pos] == c) {
            pos++
            return true
        }

        return false
    }

    fun skipWhitespace() {
        while (
            pos < input.length &&
            (input[pos] == ' ' || input[pos] == '\t')
        ) {
            pos++
        }
    }

    fun atEnd(): Boolean =
        pos == input.length

    private fun isTokenChar(c: Char): Boolean =
        c.code in 33..126 && c !in "()<>@,;:\\\"/[]?={} \t"
}
