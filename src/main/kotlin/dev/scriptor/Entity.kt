package dev.scriptor

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import dev.scriptor.server.Provider
import java.sql.*
import java.sql.JDBCType.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid

interface Entity {

    val id: Uuid
}

private data class ColumnMetadata(
    val name: String,
    val type: JDBCType,
    val notnull: Boolean,
    val primary: Boolean,
    val unique: Boolean,
    val get: (Entity) -> Any?,
    val references: KClass<*>?,
    val serialize: (Any?) -> Any?,
    val deserialize: (Any?) -> Any?,
)

private data class TableMetadata(
    val name: String,
    val columns: List<ColumnMetadata>,
    val constructor: KFunction<*>,
)

enum class ConflictMode {
    DEFAULT,
    REPLACE,
    IGNORE,
}

enum class CompareMode(val value: String) {
    EQ("="),
    NE("<>"),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
}

interface QueryNode {
    fun build(parameters: MutableList<Any?>): String
}

data class QueryNodeAnd(val nodes: List<QueryNode>) : QueryNode {
    override fun build(parameters: MutableList<Any?>): String {
        return nodes.joinToString(" and ", "(", ")") { it.build(parameters) }
    }
}

data class QueryNodeOr(val nodes: List<QueryNode>) : QueryNode {
    override fun build(parameters: MutableList<Any?>): String {
        return nodes.joinToString(" or ", "(", ")") { it.build(parameters) }
    }
}

data class QueryNodeNot(val node: QueryNode) : QueryNode {
    override fun build(parameters: MutableList<Any?>): String {
        return "(not ${node.build(parameters)})"
    }
}

data class QueryNodeCompare(val name: String, val mode: CompareMode, val value: Any?) : QueryNode {
    override fun build(parameters: MutableList<Any?>): String {
        parameters.add(value)
        return """("$name" ${mode.value} ?)"""
    }
}

data class QueryNodeIn(val name: String, val values: Iterable<Any?>) : QueryNode {
    override fun build(parameters: MutableList<Any?>): String {
        parameters.addAll(values)
        val placeholders = values.joinToString(", ", "(", ")") { "?" }
        return """("$name" in $placeholders)"""
    }
}

infix fun QueryNode.and(other: QueryNode): QueryNode {
    if (this is QueryNodeAnd && other is QueryNodeAnd) {
        return QueryNodeAnd(listOf(this.nodes, other.nodes).flatten())
    }

    if (this is QueryNodeAnd) {
        return QueryNodeAnd(listOf(this.nodes, listOf(other)).flatten())
    }

    if (other is QueryNodeAnd) {
        return QueryNodeAnd(listOf(listOf(this), other.nodes).flatten())
    }

    return QueryNodeAnd(listOf(this, other))
}

infix fun QueryNode.or(other: QueryNode): QueryNode {
    if (this is QueryNodeOr && other is QueryNodeOr) {
        return QueryNodeOr(listOf(this.nodes, other.nodes).flatten())
    }

    if (this is QueryNodeOr) {
        return QueryNodeOr(listOf(this.nodes, listOf(other)).flatten())
    }

    if (other is QueryNodeOr) {
        return QueryNodeOr(listOf(listOf(this), other.nodes).flatten())
    }

    return QueryNodeOr(listOf(this, other))
}

operator fun QueryNode.not(): QueryNode = if (this is QueryNodeNot) this.node else QueryNodeNot(this)

infix fun String.eq(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.EQ, value)
infix fun String.ne(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.NE, value)
infix fun String.lt(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.LT, value)
infix fun String.le(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.LE, value)
infix fun String.gt(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.GT, value)
infix fun String.ge(value: Any?): QueryNode = QueryNodeCompare(this, CompareMode.GE, value)

infix fun String.`in`(values: Iterable<Any?>): QueryNode = QueryNodeIn(this, values)

class EntityConnection(
    val provider: Provider,
    val connection: Connection,
    val typeMap: Map<KType, JDBCType>,
) {
    private val metadata = mutableMapOf<KClass<*>, TableMetadata>()
    private val entities = mutableMapOf<Uuid, Entity?>()

    private fun table(klass: KClass<*>): TableMetadata {
        return metadata.computeIfAbsent(klass) {
            val table = klass.findAnnotation<Table>() ?: error("class $klass is not a table")
            val name = table.value.lowercase()

            val constructor = klass.primaryConstructor ?: error("class $klass does not have a primary constructor")
            val parameters = constructor.parameters
            val properties = klass.memberProperties

            val columns = parameters.map { parameter ->
                val column = parameter.findAnnotation<Column>() ?: error("parameter $parameter is not a column")
                val name = column.value.ifEmpty { parameter.name ?: error("parameter $parameter does not have a name") }
                val unique = column.unique

                val type = parameter.type
                val notnull = !type.isMarkedNullable
                val classifier = type.classifier

                val property = properties.first { it.name == parameter.name }

                if (classifier is KClass<*> && classifier.isSubclassOf(Entity::class)) {
                    ColumnMetadata(
                        name,
                        VARCHAR,
                        notnull,
                        name == "id",
                        unique,
                        { property.getter.call(it) },
                        classifier,
                        { if (it == null) null else (it as Entity).id },
                        { if (it == null) null else (it as Entity).id },
                    )
                } else {
                    val deserialized = type.withNullability(false)

                    val jdbcType = when (classifier) {
                        Byte::class -> TINYINT
                        Short::class -> SMALLINT
                        Int::class -> INTEGER
                        Long::class -> BIGINT
                        Float::class -> FLOAT
                        Double::class -> DOUBLE
                        String::class -> VARCHAR
                        Date::class -> DATE
                        Time::class -> TIME
                        Timestamp::class -> TIMESTAMP
                        ByteArray::class -> VARBINARY
                        Boolean::class -> BOOLEAN
                        else -> when {
                            deserialized in typeMap -> typeMap[deserialized]!!

                            else -> error("no jdbc type for $deserialized")
                        }
                    }

                    val serialized = when (jdbcType) {
                        TINYINT -> typeOf<Byte>()
                        SMALLINT -> typeOf<Short>()
                        INTEGER -> typeOf<Int>()
                        BIGINT -> typeOf<Long>()
                        FLOAT -> typeOf<Float>()
                        DOUBLE -> typeOf<Double>()
                        VARCHAR -> typeOf<String>()
                        DATE -> typeOf<Date>()
                        TIME -> typeOf<Time>()
                        TIMESTAMP -> typeOf<Timestamp>()
                        VARBINARY -> typeOf<ByteArray>()
                        BOOLEAN -> typeOf<Boolean>()
                        else -> error("unsupported jdbc type $jdbcType")
                    }

                    val serialize = provider[deserialized to serialized]
                        ?: error("unsupported conversion from $deserialized to $serialized")
                    val deserialize = provider[serialized to deserialized]
                        ?: error("unsupported conversion from $serialized to $deserialized")

                    ColumnMetadata(
                        name,
                        jdbcType,
                        notnull,
                        name == "id",
                        unique,
                        { property.getter.call(it) },
                        null,
                        { if (it == null) null else context(provider) { serialize(it) } },
                        { if (it == null) null else context(provider) { deserialize(it) } },
                    )
                }
            }

            TableMetadata(
                name,
                columns,
                constructor,
            )
        }
    }

    private fun createTableStatement(table: TableMetadata): PreparedStatement {
        val constraints = mutableListOf<String>()

        val columns = table.columns.map {
            buildString {
                append(""""${it.name}"""")
                append(" ")
                append(it.type)

                if (it.notnull) append(" not null")

                if (it.primary) constraints.add("""primary key ("${it.name}")""")
                if (it.unique) constraints.add("""unique ("${it.name}")""")

                if (it.references != null) {
                    val foreignTable = table(it.references)

                    constraints.add("""foreign key ("${it.name}") references "${foreignTable.name}" (id)""")
                }
            }
        }

        val definition = listOf(columns, constraints).flatten().joinToString(", ")

        return connection.prepareStatement("""create table if not exists "${table.name}" ($definition)""")
    }

    private fun getStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }

        return connection.prepareStatement("""select $columns from "${table.name}" where id = ? limit 1""")
    }

    private fun getAllStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }

        return connection.prepareStatement("""select $columns from "${table.name}"""")
    }

    private fun createStatement(table: TableMetadata, mode: ConflictMode): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }
        val placeholders = table.columns.joinToString(", ") { "?" }

        val conflict = when (mode) {
            ConflictMode.DEFAULT -> ""
            ConflictMode.REPLACE -> "or replace"
            ConflictMode.IGNORE -> "or ignore"
        }

        return connection.prepareStatement("""insert $conflict into "${table.name}" ($columns) values ($placeholders)""")
    }

    private fun updateStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { column -> """"${column.name}" = ?""" }

        return connection.prepareStatement("""update "${table.name}" where id = ? set $columns limit 1""")
    }

    private fun deleteStatement(table: TableMetadata): PreparedStatement {
        return connection.prepareStatement("""delete from "${table.name}" where id = ? limit 1""")
    }

    private fun next(result: ResultSet, table: TableMetadata, block: (Entity) -> Unit): Boolean {
        if (!result.next()) return false

        val args = table.columns
            .mapIndexed { index, column ->
                val value = result.getObject(index + 1)

                column.deserialize(value)
            }
            .toTypedArray()

        val entity = table.constructor.call(*args) as Entity

        block(entity)

        return true
    }

    private fun put(statement: PreparedStatement, table: TableMetadata, entity: Entity, offset: Int = 0) {
        table.columns
            .forEachIndexed { index, column ->
                val value = column.get(entity)

                statement.setObject(index + offset + 1, column.serialize(value))
            }
    }

    fun createTable(klass: KClass<*>) {
        val table = table(klass)

        createTableStatement(table).use { statement -> statement.executeUpdate() }
    }

    fun get(klass: KClass<*>, id: Uuid): Entity? {
        if (id in entities) return entities[id]

        val table = table(klass)

        return getStatement(table).use { statement ->
            statement.setString(1, id.toHexDashString())

            statement.executeQuery().use { result ->
                var queried: Entity? = null

                next(result, table) {
                    entities[it.id] = it
                    queried = it
                }

                queried
            }
        }
    }

    fun getAll(klass: KClass<*>): List<Entity> {
        val table = table(klass)

        val statement = getAllStatement(table)

        return statement.use { statement ->
            statement.executeQuery().use { result ->
                val queried = mutableListOf<Entity>()

                while (
                    next(result, table) {
                        entities[it.id] = it
                        queried.add(it)
                    }
                );

                queried
            }
        }
    }

    fun create(
        entity: Entity,
        mode: ConflictMode = ConflictMode.DEFAULT,
    ): Entity? {
        val klass = entity::class
        val table = table(klass)

        return createStatement(table, mode).use { statement ->
            put(statement, table, entity)

            if (statement.executeUpdate() == 0) null else {
                entities[entity.id] = entity
                entity
            }
        }
    }

    fun create(
        klass: KClass<*>,
        block: (submit: (entity: Entity) -> Unit) -> Unit,
        mode: ConflictMode = ConflictMode.DEFAULT,
    ): List<Entity> {
        val table = table(klass)

        return createStatement(table, mode).use { statement ->
            val created = mutableListOf<Entity>()

            block { entity ->
                created.add(entity)

                put(statement, table, entity)

                statement.addBatch()
            }

            statement.executeLargeBatch()

            for (entity in created) {
                entities[entity.id] = entity
            }

            created
        }
    }

    fun update(entity: Entity): Entity? {
        val klass = entity::class
        val table = table(klass)

        return updateStatement(table).use { statement ->
            statement.setString(1, entity.id.toHexDashString())

            put(statement, table, entity, 1)

            if (statement.executeUpdate() == 0) null else {
                entities[entity.id] = entity
                entity
            }
        }
    }

    fun delete(entity: Entity): Entity? {
        val klass = entity::class
        val table = table(klass)

        return deleteStatement(table).use { statement ->
            statement.setString(1, entity.id.toHexDashString())

            if (statement.executeUpdate() == 0) null else {
                entities.remove(entity.id)
            }
        }
    }

    fun get(klass: KClass<*>, node: QueryNode): Entity? {
        val table = table(klass)

        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""select $columns from "${table.name}" where $condition limit 1""")
            .use { statement ->
                parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }

                statement.executeQuery().use { result ->
                    var queried: Entity? = null

                    next(result, table) {
                        entities[it.id] = it
                        queried = it
                    }

                    queried
                }
            }
    }

    fun getAll(klass: KClass<*>, node: QueryNode): List<Entity> {
        val table = table(klass)

        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""select $columns from "${table.name}" where $condition""")
            .use { statement ->
                parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }

                statement.executeQuery().use { result ->
                    val queried = mutableListOf<Entity>()

                    while (
                        next(result, table) {
                            entities[it.id] = it
                            queried.add(it)
                        }
                    );

                    queried
                }
            }
    }

    fun deleteAll(klass: KClass<*>, node: QueryNode): List<Entity> {
        val table = table(klass)

        val columns = table.columns.joinToString(", ") { """"${it.name}"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""delete from "${table.name}" where $condition returning $columns""")
            .use { statement ->
                parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }

                statement.executeQuery().use { result ->
                    val deleted = mutableListOf<Entity>()

                    while (
                        next(result, table) {
                            entities.remove(it.id)
                            deleted.add(it)
                        }
                    );

                    deleted
                }
            }
    }
}

inline fun <reified T : Entity> EntityConnection.createTable() {
    this.createTable(T::class)
}

inline fun <reified T : Entity> EntityConnection.get(id: Uuid): T? {
    return this.get(T::class, id) as? T
}

inline fun <reified T : Entity> EntityConnection.getAll(): List<T> {
    return this.getAll(T::class).map { it as T }
}

inline fun <reified T : Entity> EntityConnection.create(
    entity: T,
    mode: ConflictMode = ConflictMode.DEFAULT,
): T? {
    return this.create(entity as Entity, mode) as? T
}

inline fun <reified T : Entity> EntityConnection.create(
    mode: ConflictMode = ConflictMode.DEFAULT,
    noinline block: (submit: (entity: T) -> Unit) -> Unit,
): List<T> {
    return this.create(T::class, block, mode).map { it as T }
}

inline fun <reified T : Entity> EntityConnection.update(entity: T): T? {
    return this.update(entity as Entity) as? T
}

inline fun <reified T : Entity> EntityConnection.delete(entity: T): T? {
    return this.delete(entity as Entity) as? T
}

inline fun <reified T : Entity> EntityConnection.get(node: QueryNode): T? {
    return this.get(T::class, node) as? T
}

inline fun <reified T : Entity> EntityConnection.getAll(node: QueryNode): List<T> {
    return this.getAll(T::class, node).map { it as T }
}

inline fun <reified T : Entity> EntityConnection.deleteAll(node: QueryNode): List<T> {
    return this.deleteAll(T::class, node).map { it as T }
}
