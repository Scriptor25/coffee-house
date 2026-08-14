package dev.scriptor

sealed interface JsonNode {

    val entries: Set<Map.Entry<String, JsonNode>>
        get() = throw UnsupportedOperationException()

    operator fun contains(key: String): Boolean = false
    operator fun get(key: String): JsonNode = throw UnsupportedOperationException()

    val size: Int
        get() = throw UnsupportedOperationException()

    operator fun get(index: Int): JsonNode = throw UnsupportedOperationException()
    operator fun iterator(): Iterator<JsonNode> = throw UnsupportedOperationException()

    operator fun invoke(): Any? = throw UnsupportedOperationException()

    fun toMutable(): MutableJsonNode
}

sealed interface MutableJsonNode : JsonNode {

    override val entries: MutableSet<MutableMap.MutableEntry<String, MutableJsonNode>>
        get() = throw UnsupportedOperationException()

    override operator fun get(key: String): MutableJsonNode = throw UnsupportedOperationException()
    operator fun set(key: String, node: JsonNode): Unit = set(key, node.toMutable())
    operator fun set(key: String, node: MutableJsonNode): Unit = throw UnsupportedOperationException()

    override operator fun get(index: Int): MutableJsonNode = throw UnsupportedOperationException()
    override operator fun iterator(): MutableIterator<MutableJsonNode> = throw UnsupportedOperationException()
    operator fun set(index: Int, node: JsonNode): Unit = set(index, node.toMutable())
    operator fun set(index: Int, node: MutableJsonNode): Unit = throw UnsupportedOperationException()

    fun add(node: JsonNode): Unit = add(node.toMutable())
    fun add(node: MutableJsonNode): Unit = throw UnsupportedOperationException()

    operator fun invoke(value: Any?): Unit = throw UnsupportedOperationException()
}

private open class JsonObjectNode(
    private val nodes: Map<String, JsonNode> = mapOf(),
) : JsonNode {

    override val entries: Set<Map.Entry<String, JsonNode>>
        get() = nodes.entries

    override fun contains(key: String): Boolean {
        return key in nodes
    }

    override fun get(key: String): JsonNode {
        return nodes[key] ?: JsonValueNode()
    }

    override fun toMutable(): MutableJsonNode {
        return MutableJsonObjectNode(nodes.entries.associate { it.key to it.value.toMutable() }.toMutableMap())
    }

    override fun toString(): String {
        return nodes.entries.joinToString(",", "{", "}") { (key, value) -> """${escape(key)}:$value""" }
    }
}

private class MutableJsonObjectNode(
    private val nodes: MutableMap<String, MutableJsonNode> = mutableMapOf(),
) : MutableJsonNode, JsonObjectNode(nodes) {

    override val entries: MutableSet<MutableMap.MutableEntry<String, MutableJsonNode>>
        get() = nodes.entries

    override fun get(key: String): MutableJsonNode {
        return nodes.computeIfAbsent(key) { MutableJsonValueNode() }
    }

    override fun set(key: String, node: MutableJsonNode) {
        nodes[key] = node
    }

    override fun toMutable(): MutableJsonNode = this
}

private open class JsonArrayNode(
    private val nodes: List<JsonNode> = listOf(),
) : JsonNode {

    override val size: Int
        get() = nodes.size

    override fun get(index: Int): JsonNode {
        return nodes[index]
    }

    override fun iterator(): Iterator<JsonNode> {
        return nodes.iterator()
    }

    override fun toString(): String {
        return nodes.joinToString(",", "[", "]")
    }

    override fun toMutable(): MutableJsonNode {
        return MutableJsonArrayNode(nodes.map { it.toMutable() }.toMutableList())
    }
}

private class MutableJsonArrayNode(
    private val nodes: MutableList<MutableJsonNode> = mutableListOf(),
) : MutableJsonNode, JsonArrayNode(nodes) {

    override fun get(index: Int): MutableJsonNode {
        return nodes[index]
    }

    override fun set(index: Int, node: MutableJsonNode) {
        nodes[index] = node
    }

    override fun add(node: MutableJsonNode) {
        nodes.add(node)
    }

    override fun iterator(): MutableIterator<MutableJsonNode> {
        return nodes.iterator()
    }

    override fun toMutable(): MutableJsonNode = this
}

private fun escape(value: String): String {
    val sanitized = StringBuilder()

    for (c in value) {
        val x = when (c) {
            '\"' -> "\\\""
            '\\' -> "\\\\"
            '/' -> "\\/"
            '\b' -> "\\b"
            0x0C.toChar() -> "\\f"
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"

            else -> if (c.code !in 0x20..0xFF) {
                "\\u${c.code.toHexString().padStart(4, '0')}"
            } else c.toString()
        }

        sanitized.append(x)
    }

    return """"$sanitized""""
}

private open class JsonValueNode(
    private val value: Any? = null,
) : JsonNode {

    override fun invoke(): Any? {
        return value
    }

    override fun toString(): String {
        return when (value) {
            null -> "null"
            is Boolean -> value.toString()
            is Number -> value.toString()
            else -> escape(value.toString())
        }
    }

    override fun toMutable(): MutableJsonNode = MutableJsonValueNode(value)
}

private class MutableJsonValueNode(
    private var value: Any? = null,
) : MutableJsonNode, JsonValueNode(value) {

    override fun invoke(): Any? {
        return value
    }

    override fun invoke(value: Any?) {
        this.value = value
    }

    override fun toString(): String {
        return when (value) {
            null -> "null"
            is Boolean -> value.toString()
            is Number -> value.toString()
            else -> escape(value.toString())
        }
    }

    override fun toMutable(): MutableJsonNode = this
}

fun emptyJsonObject(): JsonNode {
    return JsonObjectNode()
}

fun emptyJsonArray(): JsonNode {
    return JsonArrayNode()
}

fun jsonOf(vararg entries: Pair<String, JsonNode>): JsonNode {
    return JsonObjectNode(mapOf(*entries))
}

fun mutableJsonOf(vararg entries: Pair<String, JsonNode>): MutableJsonNode {
    return MutableJsonObjectNode(mutableMapOf(*entries.map { it.first to it.second.toMutable() }.toTypedArray()))
}

fun jsonOf(vararg entries: JsonNode): JsonNode {
    return JsonArrayNode(listOf(*entries))
}

fun mutableJsonOf(vararg entries: JsonNode): MutableJsonNode {
    return MutableJsonArrayNode(mutableListOf(*entries.map { it.toMutable() }.toTypedArray()))
}

fun jsonOf(value: Any?): JsonNode {
    return JsonValueNode(value)
}

fun mutableJsonOf(value: Any?): MutableJsonNode {
    return MutableJsonValueNode(value)
}

inline fun <reified T> JsonNode.get() = this() as T

private enum class TokenType {
    NONE,
    WHITESPACE,
    IDENTIFIER,
    STRING,
    NUMBER,
    OTHER,
}

private data class Context(val text: String, var pointer: Int = 0) {

    private data class Token(
        val type: TokenType,
        val value: String,
    )

    private var token: Token = parseToken()

    private enum class State {
        NONE,
        WHITESPACE,
        IDENTIFIER,
        STRING,
        NUMBER,
        NUMBER_INTEGER,
        NUMBER_FRACTION,
        NUMBER_EXPONENT,
        NUMBER_EXPONENT_INTEGER,
    }

    private fun parseToken(): Token {
        var state = State.NONE
        val value = StringBuilder()

        val split = { value.append(text[pointer++]) }
        val c = { text[pointer] }
        val n = { text[pointer++] }

        while (pointer < text.length) {
            when (state) {
                State.NONE -> state = when (val x = c()) {
                    '\"' -> {
                        pointer++
                        State.STRING
                    }

                    '-' -> {
                        split()
                        State.NUMBER
                    }

                    '0' -> {
                        State.NUMBER
                    }

                    else -> when {
                        x.isWhitespace() -> State.WHITESPACE
                        x.isDigit() -> State.NUMBER_INTEGER
                        x.isLetter() -> State.IDENTIFIER

                        else -> {
                            split()
                            return Token(TokenType.OTHER, value.toString())
                        }
                    }
                }

                State.WHITESPACE -> {
                    if (c().isWhitespace()) {
                        split()
                    } else {
                        return Token(TokenType.WHITESPACE, value.toString())
                    }
                }

                State.IDENTIFIER -> {
                    if (c().isLetter()) {
                        split()
                    } else {
                        return Token(TokenType.IDENTIFIER, value.toString())
                    }
                }

                State.STRING -> {
                    when (val x = n()) {
                        '\"' -> return Token(TokenType.STRING, value.toString())

                        '\\' -> {
                            when (val x = n()) {
                                '\"' -> value.append('\"')
                                '\\' -> value.append('\\')
                                '/' -> value.append('/')
                                'b' -> value.append('\b')
                                'f' -> value.append(0x0C.toChar())
                                'n' -> value.append('\n')
                                'r' -> value.append('\r')
                                't' -> value.append('\t')

                                'u' -> {
                                    val b0 = n()
                                    val b1 = n()
                                    val b2 = n()
                                    val b3 = n()

                                    value.append("$b0$b1$b2$b3".toInt(16).toChar())
                                }

                                else -> error("unsupported character '$x'")
                            }
                        }

                        else -> value.append(x)
                    }
                }

                State.NUMBER -> {
                    state = when (c()) {
                        '0' -> {
                            split()
                            when (c()) {
                                '.' -> {
                                    split()
                                    State.NUMBER_FRACTION
                                }

                                'E', 'e' -> {
                                    split()
                                    State.NUMBER_EXPONENT
                                }

                                else -> return Token(TokenType.NUMBER, value.toString())
                            }
                        }

                        else -> State.NUMBER_INTEGER
                    }
                }

                State.NUMBER_INTEGER -> {
                    if (c().isDigit()) {
                        split()
                    } else state = when (c()) {
                        '.' -> {
                            split()
                            State.NUMBER_FRACTION
                        }

                        'E', 'e' -> {
                            split()
                            State.NUMBER_EXPONENT
                        }

                        else -> return Token(TokenType.NUMBER, value.toString())
                    }
                }

                State.NUMBER_FRACTION -> {
                    if (c().isDigit()) {
                        split()
                    } else state = when (c()) {
                        'E', 'e' -> {
                            split()
                            State.NUMBER_EXPONENT
                        }

                        else -> return Token(TokenType.NUMBER, value.toString())
                    }
                }

                State.NUMBER_EXPONENT -> {
                    when (c()) {
                        '-', '+' -> split()
                    }
                    state = State.NUMBER_EXPONENT_INTEGER
                }

                State.NUMBER_EXPONENT_INTEGER -> {
                    if (c().isDigit()) {
                        split()
                    } else {
                        return Token(TokenType.NUMBER, value.toString())
                    }
                }
            }
        }

        return when (state) {
            State.WHITESPACE -> Token(TokenType.WHITESPACE, value.toString())
            State.IDENTIFIER -> Token(TokenType.IDENTIFIER, value.toString())
            State.NUMBER -> Token(TokenType.NUMBER, value.toString())
            State.STRING -> error("string is missing closing double-quote")

            else -> Token(TokenType.NONE, value.toString())
        }
    }

    fun skipToken(): String {
        val value = token.value
        token = parseToken()
        return value
    }

    fun atToken(type: TokenType, value: String? = null): Boolean {
        if (token.type != type) return false
        if (value == null) return true
        return token.value == value
    }

    fun skipToken(type: TokenType, value: String? = null): Boolean {
        return if (atToken(type, value)) {
            token = parseToken()
            true
        } else false
    }

    fun expectToken(type: TokenType): String {
        return if (atToken(type)) {
            val value = token.value
            token = parseToken()
            value
        } else error("type = $type, value = ... <---> type = ${token.type}, value = '${token.value}'")
    }

    fun expectToken(type: TokenType, value: String) {
        if (skipToken(type, value)) return
        error("type = $type, value = '$value' <---> type = ${token.type}, value = '${token.value}'")
    }
}

private fun parseJsonObject(context: Context): JsonNode {
    context.expectToken(TokenType.OTHER, "{")
    context.skipToken(TokenType.WHITESPACE)

    if (context.atToken(TokenType.OTHER, "}")) return JsonObjectNode()

    val nodes = mutableMapOf<String, JsonNode>()

    do {
        context.skipToken(TokenType.WHITESPACE)

        val key = context.expectToken(TokenType.STRING)

        context.skipToken(TokenType.WHITESPACE)
        context.expectToken(TokenType.OTHER, ":")

        val value = parseJsonValue(context)

        nodes[key] = value
    } while (context.skipToken(TokenType.OTHER, ","))

    context.expectToken(TokenType.OTHER, "}")

    return JsonObjectNode(nodes)
}

private fun parseJsonArray(context: Context): JsonNode {
    context.expectToken(TokenType.OTHER, "[")
    context.skipToken(TokenType.WHITESPACE)

    if (context.skipToken(TokenType.OTHER, "]")) return JsonArrayNode()

    val nodes = mutableListOf<JsonNode>()

    do {
        val value = parseJsonValue(context)

        nodes.add(value)
    } while (context.skipToken(TokenType.OTHER, ","))

    context.expectToken(TokenType.OTHER, "]")

    return JsonArrayNode(nodes)
}

private fun parseJsonValue(context: Context): JsonNode {
    context.skipToken(TokenType.WHITESPACE)

    val value = when {
        context.skipToken(TokenType.IDENTIFIER, "null") -> JsonValueNode()
        context.skipToken(TokenType.IDENTIFIER, "false") -> JsonValueNode(false)
        context.skipToken(TokenType.IDENTIFIER, "true") -> JsonValueNode(true)

        context.atToken(TokenType.STRING) -> JsonValueNode(context.skipToken())
        context.atToken(TokenType.NUMBER) -> JsonValueNode(context.skipToken().toDouble())

        context.atToken(TokenType.OTHER, "{") -> parseJsonObject(context)
        context.atToken(TokenType.OTHER, "[") -> parseJsonArray(context)

        else -> throw UnsupportedOperationException()
    }

    context.skipToken(TokenType.WHITESPACE)

    return value
}

fun parseJson(text: String): JsonNode {
    return parseJsonValue(Context(text))
}
