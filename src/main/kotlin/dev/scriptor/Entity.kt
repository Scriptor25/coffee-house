package dev.scriptor

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.Table
import dev.scriptor.server.Provider
import java.sql.*
import java.sql.JDBCType.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.uuid.Uuid

interface Entity {

    val id: Uuid
}

private data class ColumnMetadata(
    val parameter: KParameter,
    val name: String,
    val type: JDBCType,
    val typeClass: KClass<*>,
    val notnull: Boolean,
    val primary: Boolean,
    val unique: String,
    val index: Int,
    val get: (Entity) -> Any?,
    val references: KClass<*>?,
    val serialize: (Any?) -> Any?,
    val deserialize: (Any?) -> Any?,
) {
    override fun toString(): String {
        return name
    }
}

private data class TableMetadata(
    val klass: KClass<*>,
    val name: String,
    val columns: List<ColumnMetadata>,
    val constructor: KFunction<*>,
) {
    override fun toString(): String {
        return name
    }
}

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
            val table = klass.findAnnotation<Table>() ?: error("$klass is not a table")
            val name = table.name.lowercase()

            val constructor = klass.primaryConstructor ?: error("$klass does not have a primary constructor")
            val parameters = constructor.parameters
            val properties = klass.memberProperties

            val columns = parameters.filter { it.hasAnnotation<Column>() }.map { parameter ->
                val column = parameter.findAnnotation<Column>()!!
                val name = column.name.ifEmpty { parameter.name ?: error("$parameter does not have a name") }
                val unique = column.unique

                val type = parameter.type
                val notnull = !type.isMarkedNullable
                val classifier = type.classifier

                val property = properties.first { it.name == parameter.name }

                if (classifier is KClass<*> && classifier.isSubclassOf(Entity::class)) {
                    ColumnMetadata(
                        parameter,
                        name,
                        VARCHAR,
                        String::class,
                        notnull,
                        name == "id",
                        unique,
                        parameter.index,
                        { property.getter.call(it) },
                        classifier,
                        { if (it == null) null else (it as Entity).id },
                        { if (it == null) null else get(classifier, Uuid.parseHexDash(it as String)) },
                    )
                } else {
                    val dataType = type.withNullability(false)

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
                            dataType in typeMap -> typeMap[dataType]!!

                            else -> error("no jdbc type for $dataType")
                        }
                    }

                    val typeClass = when (jdbcType) {
                        TINYINT -> Byte::class
                        SMALLINT -> Short::class
                        INTEGER -> Int::class
                        BIGINT -> Long::class
                        FLOAT -> Float::class
                        DOUBLE -> Double::class
                        VARCHAR -> String::class
                        DATE -> Date::class
                        TIME -> Time::class
                        TIMESTAMP -> Timestamp::class
                        VARBINARY -> ByteArray::class
                        BOOLEAN -> Boolean::class
                        else -> error("unsupported jdbc type $jdbcType")
                    }

                    val serialType = typeClass.createType()

                    val serialize = provider[dataType to serialType]
                        ?: error("unsupported conversion from $dataType to $serialType")
                    val deserialize = provider[serialType to dataType]
                        ?: error("unsupported conversion from $serialType to $dataType")

                    ColumnMetadata(
                        parameter,
                        name,
                        jdbcType,
                        typeClass,
                        notnull,
                        name == "id",
                        unique,
                        parameter.index,
                        { property.getter.call(it) },
                        null,
                        { if (it == null) null else context(provider) { serialize(it) } },
                        { if (it == null) null else context(provider) { deserialize(it) } },
                    )
                }
            }

            TableMetadata(
                klass,
                name,
                columns,
                constructor,
            )
        }
    }

    private fun createTableStatement(table: TableMetadata): PreparedStatement {
        val constraints = mutableListOf<String>()

        val unique = mutableMapOf<String, MutableList<String>>()

        val columns = table.columns.map {
            buildString {
                append(""""$it"""")
                append(" ")
                append(it.type)

                if (it.notnull) append(" not null")

                if (it.primary) constraints.add("""primary key ("$it")""")

                if (it.unique.isNotEmpty()) {
                    unique.computeIfAbsent(it.unique) { mutableListOf() }.add(it.name)
                }

                if (it.references != null) {
                    val foreignTable = table(it.references)

                    constraints.add("""foreign key ("$it") references "$foreignTable" (id)""")
                }
            }
        }

        unique.forEach { (name, columns) ->
            constraints.add("""constraint "$name" unique ${columns.joinToString(", ", "(", ")") { """"$it"""" }}""")
        }

        val definition = listOf(columns, constraints).flatten().joinToString(", ", "(", ")")

        return connection.prepareStatement("""create table if not exists "$table" $definition""")
    }

    private fun getStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"$it"""" }

        return connection.prepareStatement("""select $columns from "$table" where id = ? limit 1""")
    }

    private fun getAllStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"$it"""" }

        return connection.prepareStatement("""select $columns from "$table"""")
    }

    private fun createStatement(table: TableMetadata, mode: ConflictMode): PreparedStatement {
        val columns = table.columns.joinToString(", ") { """"$it"""" }
        val placeholders = table.columns.joinToString(", ") { "?" }

        val conflict = when (mode) {
            ConflictMode.DEFAULT -> ""
            ConflictMode.REPLACE -> "or replace"
            ConflictMode.IGNORE -> "or ignore"
        }

        return connection.prepareStatement("""insert $conflict into "$table" ($columns) values ($placeholders)""")
    }

    private fun updateStatement(table: TableMetadata): PreparedStatement {
        val columns = table.columns.joinToString(", ") { column -> """"$column" = ?""" }

        return connection.prepareStatement("""update "$table" where id = ? set $columns limit 1""")
    }

    private fun deleteStatement(table: TableMetadata): PreparedStatement {
        return connection.prepareStatement("""delete from "$table" where id = ? limit 1""")
    }

    private fun next(result: ResultSet, table: TableMetadata, block: (Entity) -> Unit): Boolean {
        if (!result.next()) return false

        var id: Uuid? = null
        val args = Array<Any?>(table.constructor.parameters.size) { null }

        table.columns.forEach { column ->
            val value = result.getObject(column.name, column.typeClass.javaObjectType)

            val x = column.deserialize(value)

            if (column.primary) id = x as Uuid

            args[column.index] = x
        }

        val entity = table.constructor.call(*args) as Entity

        block(entity)

        return true
    }

    private fun put(statement: PreparedStatement, table: TableMetadata, entity: Entity, offset: Int = 0) {
        table.columns.forEachIndexed { index, column ->
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

        val columns = table.columns.joinToString(", ") { """"$it"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""select $columns from "$table" where $condition limit 1""")
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

        val columns = table.columns.joinToString(", ") { """"$it"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""select $columns from "$table" where $condition""")
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

        val columns = table.columns.joinToString(", ") { """"$it"""" }

        val parameters = mutableListOf<Any?>()
        val condition = node.build(parameters)

        return connection.prepareStatement("""delete from "$table" where $condition returning $columns""")
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
