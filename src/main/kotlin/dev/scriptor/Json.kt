package dev.scriptor

import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

enum class JsonNodeType {
    NULL,
    BOOLEAN,
    NUMBER,
    STRING,
    OBJECT,
    ARRAY,
}

sealed interface JsonNode {

    val type: JsonNodeType

    operator fun not(): Boolean = type == JsonNodeType.NULL

    fun toMutable(): MutableJsonNode

    fun toJson(): String
}

sealed interface MutableJsonNode : JsonNode {

    override fun toMutable(): MutableJsonNode = this
}

sealed interface JsonObjectNode : JsonNode {

    override val type: JsonNodeType
        get() = JsonNodeType.OBJECT

    val nodes: Map<String, JsonNode>

    val entries: Set<Map.Entry<String, JsonNode>>
        get() = nodes.entries

    operator fun contains(key: String): Boolean {
        return key in nodes
    }

    operator fun get(key: String): JsonNode {
        return nodes[key] ?: JsonNullNodeImpl()
    }

    override fun toMutable(): MutableJsonObjectNode {
        return MutableJsonObjectNodeImpl(nodes.entries.associate { it.key to it.value.toMutable() }.toMutableMap())
    }

    override fun toJson(): String {
        return nodes.entries.joinToString(",", "{", "}") { (key, value) -> """${escape(key)}:$value""" }
    }
}

sealed interface MutableJsonObjectNode : MutableJsonNode, JsonObjectNode {

    override val nodes: MutableMap<String, MutableJsonNode>

    override val entries: MutableSet<MutableMap.MutableEntry<String, MutableJsonNode>>
        get() = nodes.entries

    override fun get(key: String): MutableJsonNode {
        return nodes.computeIfAbsent(key) { JsonNullNodeImpl() }
    }

    operator fun set(key: String, node: JsonNode) {
        nodes[key] = node.toMutable()
    }

    override fun toMutable(): MutableJsonObjectNode = this
}

private class JsonObjectNodeImpl(
    override val nodes: Map<String, JsonNode> = mapOf(),
) : JsonObjectNode {
    override fun toString(): String = toJson()
}

private class MutableJsonObjectNodeImpl(
    override val nodes: MutableMap<String, MutableJsonNode> = mutableMapOf(),
) : MutableJsonObjectNode {
    override fun toString(): String = toJson()
}

sealed interface JsonArrayNode : JsonNode, Iterable<JsonNode> {

    override val type: JsonNodeType
        get() = JsonNodeType.ARRAY

    val nodes: List<JsonNode>

    val size: Int
        get() = nodes.size

    operator fun get(index: Int): JsonNode {
        return nodes[index]
    }

    override fun iterator(): Iterator<JsonNode> {
        return nodes.iterator()
    }

    override fun toJson(): String {
        return nodes.joinToString(",", "[", "]")
    }

    override fun toMutable(): MutableJsonArrayNode {
        return MutableJsonArrayNodeImpl(nodes.map { it.toMutable() }.toMutableList())
    }
}

sealed interface MutableJsonArrayNode : MutableJsonNode, JsonArrayNode {

    override val nodes: MutableList<MutableJsonNode>

    override fun get(index: Int): MutableJsonNode {
        return nodes[index]
    }

    operator fun set(index: Int, node: JsonNode) {
        nodes[index] = node.toMutable()
    }

    fun add(node: JsonNode) {
        nodes.add(node.toMutable())
    }

    override fun iterator(): MutableIterator<MutableJsonNode> {
        return nodes.iterator()
    }

    override fun toMutable(): MutableJsonArrayNode = this
}

private class JsonArrayNodeImpl(
    override val nodes: List<JsonNode> = listOf(),
) : JsonArrayNode {
    override fun toString(): String = toJson()
}

private class MutableJsonArrayNodeImpl(
    override val nodes: MutableList<MutableJsonNode> = mutableListOf(),
) : MutableJsonArrayNode {
    override fun toString(): String = toJson()
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

sealed interface JsonValueNode<out T> : JsonNode, MutableJsonNode {

    val value: T

    fun invoke(): T = value

    override fun toMutable(): MutableJsonNode = this
}

sealed interface JsonNullNode : JsonValueNode<Nothing?> {

    override val type
        get() = JsonNodeType.NULL

    override fun toJson(): String = "null"
}

private class JsonNullNodeImpl : JsonNullNode {
    override val value: Nothing? = null

    override fun toString(): String = toJson()
}

sealed interface JsonBooleanNode : JsonValueNode<Boolean> {

    override val type
        get() = JsonNodeType.BOOLEAN

    override fun toJson(): String = value.toString()
}

private class JsonBooleanNodeImpl(
    override val value: Boolean,
) : JsonBooleanNode {
    override fun toString(): String = toJson()
}

sealed interface JsonNumberNode : JsonValueNode<Number> {

    override val type
        get() = JsonNodeType.NUMBER

    override fun toJson(): String = value.toString()
}

private class JsonNumberNodeImpl(
    override val value: Number,
) : JsonNumberNode {
    override fun toString(): String = toJson()
}

sealed interface JsonStringNode : JsonValueNode<String> {

    override val type
        get() = JsonNodeType.STRING

    override fun toJson(): String = escape(value)
}

private class JsonStringNodeImpl(
    override val value: String,
) : JsonStringNode {
    override fun toString(): String = toJson()
}

fun emptyJsonObject(): JsonObjectNode {
    return JsonObjectNodeImpl()
}

fun emptyJsonArray(): JsonArrayNode {
    return JsonArrayNodeImpl()
}

fun jsonOf(vararg entries: Pair<String, JsonNode>): JsonObjectNode {
    return JsonObjectNodeImpl(mapOf(*entries))
}

fun mutableJsonOf(vararg entries: Pair<String, JsonNode>): MutableJsonObjectNode {
    return MutableJsonObjectNodeImpl(mutableMapOf(*entries.map { it.first to it.second.toMutable() }.toTypedArray()))
}

fun jsonOf(vararg entries: JsonNode): JsonArrayNode {
    return JsonArrayNodeImpl(listOf(*entries))
}

fun mutableJsonOf(vararg entries: JsonNode): MutableJsonArrayNode {
    return MutableJsonArrayNodeImpl(mutableListOf(*entries.map { it.toMutable() }.toTypedArray()))
}

fun jsonNull(): JsonNullNode {
    return JsonNullNodeImpl()
}

fun jsonOf(value: Boolean): JsonBooleanNode {
    return JsonBooleanNodeImpl(value)
}

fun jsonOf(value: Boolean?): JsonValueNode<Boolean?> {
    return if (value == null) JsonNullNodeImpl() else JsonBooleanNodeImpl(value)
}

fun jsonOf(value: Number): JsonNumberNode {
    return JsonNumberNodeImpl(value)
}

fun jsonOf(value: Number?): JsonValueNode<Number?> {
    return if (value == null) JsonNullNodeImpl() else JsonNumberNodeImpl(value)
}

fun jsonOf(value: String): JsonStringNode {
    return JsonStringNodeImpl(value)
}

fun jsonOf(value: String?): JsonValueNode<String?> {
    return if (value == null) JsonNullNodeImpl() else JsonStringNodeImpl(value)
}

fun jsonOf(value: Any?): JsonValueNode<Any?> {
    return when (value) {
        null -> JsonNullNodeImpl()
        is Boolean -> JsonBooleanNodeImpl(value)
        is Number -> JsonNumberNodeImpl(value)
        is String -> JsonStringNodeImpl(value)
        else -> error("unexpected value type '${value::class}'")
    }
}

fun jsonObject(block: MutableJsonObjectNode.() -> Unit): MutableJsonObjectNode {
    val node = MutableJsonObjectNodeImpl()
    node.apply(block)
    return node
}

fun jsonArray(block: MutableJsonArrayNode.() -> Unit): MutableJsonArrayNode {
    val node = MutableJsonArrayNodeImpl()
    node.apply(block)
    return node
}

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

    if (context.atToken(TokenType.OTHER, "}")) {
        return JsonObjectNodeImpl()
    }

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

    return JsonObjectNodeImpl(nodes)
}

private fun parseJsonArray(context: Context): JsonNode {
    context.expectToken(TokenType.OTHER, "[")
    context.skipToken(TokenType.WHITESPACE)

    if (context.skipToken(TokenType.OTHER, "]")) {
        return JsonArrayNodeImpl()
    }

    val nodes = mutableListOf<JsonNode>()

    do {
        val value = parseJsonValue(context)

        nodes.add(value)
    } while (context.skipToken(TokenType.OTHER, ","))

    context.expectToken(TokenType.OTHER, "]")

    return JsonArrayNodeImpl(nodes.toList())
}

private fun parseJsonValue(context: Context): JsonNode {
    context.skipToken(TokenType.WHITESPACE)

    val value = when {
        context.skipToken(TokenType.IDENTIFIER, "null") -> JsonNullNodeImpl()
        context.skipToken(TokenType.IDENTIFIER, "false") -> JsonBooleanNodeImpl(false)
        context.skipToken(TokenType.IDENTIFIER, "true") -> JsonBooleanNodeImpl(true)

        context.atToken(TokenType.NUMBER) -> JsonNumberNodeImpl(context.skipToken().toDouble())
        context.atToken(TokenType.STRING) -> JsonStringNodeImpl(context.skipToken())

        context.atToken(TokenType.OTHER, "{") -> parseJsonObject(context)
        context.atToken(TokenType.OTHER, "[") -> parseJsonArray(context)

        else -> error("failed to parse json value")
    }

    context.skipToken(TokenType.WHITESPACE)

    return value
}

fun parseJson(text: String): JsonNode {
    return parseJsonValue(Context(text))
}

typealias JsonCast = (JsonNode) -> Any?

private fun cast(name: String, node: JsonNode, type: KType, map: Map<KType, JsonCast>): Any? {
    if (node is JsonNullNode && type.isMarkedNullable) {
        return null
    }

    val convert = map[type]
    if (convert != null) {
        return convert(node)
    }

    return when (val c = type.classifier) {
        Boolean::class ->
            when (node) {
                is JsonBooleanNode -> node.value
                else -> error("invalid node '$name'")
            }

        Number::class ->
            when (node) {
                is JsonNumberNode -> node.value
                else -> error("invalid node '$name'")
            }

        String::class ->
            when (node) {
                is JsonStringNode -> node.value
                else -> error("invalid node '$name'")
            }

        List::class ->
            when (node) {
                is JsonArrayNode -> {
                    node.mapIndexed { index, subnode ->
                        cast(
                            "$name[$index]",
                            subnode,
                            type.arguments[0].type!!,
                            map,
                        )
                    }
                }

                else -> error("invalid node '$name'")
            }

        is KClass<*> ->
            when (node) {
                is JsonObjectNode -> {
                    when (val constructor = c.primaryConstructor) {
                        null -> {
                            val instance = c.createInstance()

                            for (property in c.memberProperties) {
                                if (property !is KMutableProperty<*>) continue

                                val subnode = node[property.name]
                                val value = cast(
                                    "$name.${property.name}",
                                    subnode,
                                    property.returnType,
                                    map,
                                )

                                property.setter.call(instance, value)
                            }

                            instance
                        }

                        else -> {
                            val args = mutableMapOf<KParameter, Any?>()

                            for (parameter in constructor.parameters) {
                                val parameterName = parameter.name
                                    ?: continue

                                val subnode = node[parameterName]

                                if (parameter.isOptional && subnode is JsonNullNode) {
                                    continue
                                }

                                val value = cast(
                                    "$name.${parameterName}",
                                    subnode,
                                    parameter.type,
                                    map,
                                )

                                args[parameter] = value
                            }

                            constructor.callBy(args)
                        }
                    }
                }

                else -> error("invalid node '$name'")
            }

        else -> error("invalid type '$type' for node '$name'")
    }
}

fun JsonNode.cast(type: KType, map: Map<KType, JsonCast> = emptyMap()): Any? {
    return cast("<root>", this, type, map)
}

inline fun <reified T> JsonNode.cast(map: Map<KType, JsonCast> = emptyMap()): T {
    return cast(typeOf<T>(), map) as T
}
