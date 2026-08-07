package dev.scriptor

interface JsonNode {

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

interface MutableJsonNode : JsonNode {

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

open class JsonObjectNode(
    private val nodes: Map<String, JsonNode> = mapOf(),
) : JsonNode {

    override val entries: Set<Map.Entry<String, JsonNode>>
        get() = nodes.entries

    override fun contains(key: String): Boolean {
        return key in nodes
    }

    override fun get(key: String): JsonNode {
        return nodes[key] ?: JsonDataNode()
    }

    override fun toMutable(): MutableJsonNode {
        return MutableJsonObjectNode(nodes.entries.associate { it.key to it.value.toMutable() }.toMutableMap())
    }

    override fun toString(): String {
        return nodes.entries.joinToString(",", "{", "}") { (key, value) -> """"$key":$value""" }
    }
}

data class MutableJsonObjectNode(
    private val nodes: MutableMap<String, MutableJsonNode> = mutableMapOf(),
) : MutableJsonNode, JsonObjectNode(nodes) {

    override val entries: MutableSet<MutableMap.MutableEntry<String, MutableJsonNode>>
        get() = nodes.entries

    override fun get(key: String): MutableJsonNode {
        return nodes.computeIfAbsent(key) { MutableJsonDataNode() }
    }

    override fun set(key: String, node: MutableJsonNode) {
        nodes[key] = node
    }

    override fun toMutable(): MutableJsonNode = this
}

open class JsonArrayNode(
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

data class MutableJsonArrayNode(
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

open class JsonDataNode(
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
            else -> "\"$value\""
        }
    }

    override fun toMutable(): MutableJsonNode = MutableJsonDataNode(value)
}

data class MutableJsonDataNode(
    private var value: Any? = null,
) : MutableJsonNode, JsonDataNode(value) {

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
            else -> "\"$value\""
        }
    }

    override fun toMutable(): MutableJsonNode = this
}

fun jsonOf(vararg entries: Pair<String, JsonNode>): JsonObjectNode {
    return JsonObjectNode(mapOf(*entries))
}

fun mutableJsonOf(vararg entries: Pair<String, JsonNode>): MutableJsonObjectNode {
    return MutableJsonObjectNode(mutableMapOf(*entries.map { it.first to it.second.toMutable() }.toTypedArray()))
}

fun jsonOf(vararg entries: JsonNode): JsonArrayNode {
    return JsonArrayNode(listOf(*entries))
}

fun mutableJsonOf(vararg entries: JsonNode): MutableJsonArrayNode {
    return MutableJsonArrayNode(mutableListOf(*entries.map { it.toMutable() }.toTypedArray()))
}

fun jsonOf(value: Any?): JsonDataNode {
    return JsonDataNode(value)
}

fun mutableJsonOf(value: Any?): MutableJsonDataNode {
    return MutableJsonDataNode(value)
}

inline fun <reified T> JsonNode.get() = this() as T
