package dev.scriptor

import dev.scriptor.annotation.*
import java.sql.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

sealed interface Value {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>)
}

sealed interface Node {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>)
}

enum class Order { ASC, DESC }

data class TableRef(val name: String) {
    override fun toString(): String = name
}

data class ColumnRef(val table: TableRef, val name: String) : Value {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append(this)
    }

    override fun toString(): String = "$table.$name"
}

data class ColumnDef(
    val name: String,
    val type: String,
    val notNull: Boolean = false,
) {
    override fun toString(): String = buildString {
        append(name)
        append(' ')
        append(type)

        if (notNull) {
            append(" not null")
        }
    }
}

data class ConstraintDef(
    val name: String? = null,
    val constraint: Constraint,
) {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        if (name != null) {
            sql
                .append("constraint ")
                .append(name)
        }

        constraint.generate(sql, parameters)
    }
}

enum class CreateTableMod(val value: String) {
    IF_NOT_EXISTS("if not exists");

    override fun toString(): String = value
}

private data class CreateNode(
    val modifiers: List<CreateTableMod>,
    val table: TableRef,
    val columns: List<ColumnDef>,
    val constraints: List<ConstraintDef>,
) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("create table ")

        for (modifier in modifiers) {
            sql.append(modifier).append(' ')
        }

        sql.append(table)

        if (columns.isNotEmpty() || constraints.isNotEmpty()) {
            sql.append(" (")

            for ((index, column) in columns.withIndex()) {
                if (index > 0) sql.append(", ")
                sql.append(column)
            }

            if (columns.isNotEmpty() && constraints.isNotEmpty()) {
                sql.append(", ")
            }

            for ((index, constraint) in constraints.withIndex()) {
                if (index > 0) sql.append(", ")
                constraint.generate(sql, parameters)
            }

            sql.append(')')
        }
    }
}

private data class AlterNode(val table: TableRef) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql
            .append("alter table ")
            .append(table)
    }
}

private data class AddNode(val name: String?, val constraint: Constraint) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("add ")

        if (name != null) {
            sql
                .append("constraint ")
                .append(name)
                .append(' ')
        }

        constraint.generate(sql, parameters)
    }
}

private data class SelectNode(val columns: List<ColumnRef>) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("select ")

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column)
        }
    }
}

private data class FromNode(val table: TableRef) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql
            .append("from ")
            .append(table)
    }
}

private data class WhereNode(val condition: Condition) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("where ")

        condition.generate(sql, parameters)
    }
}

private data class OrderNode(val column: ColumnRef, val order: Order) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("order by ")
        sql.append(column)
        sql.append(' ').append(order)
    }
}

private data class LimitNode(val limit: Int) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("limit ?")

        parameters.add(limit)
    }
}

private data class InsertNode(
    val table: TableRef,
    val columns: List<ColumnRef>,
    val values: List<Any?>,
) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql
            .append("insert into ")
            .append(table)

        if (columns.isNotEmpty()) {
            sql.append(" (")

            for ((index, column) in columns.withIndex()) {
                if (index > 0) sql.append(", ")
                sql.append(column.name)
            }

            sql.append(')')
        }

        sql.append(" values (")

        for ((index, value) in values.withIndex()) {
            if (index > 0) sql.append(", ")
            if (columns.isNotEmpty()) {
                named[columns[index].toString()] = parameters.size
            }
            sql.append('?')
            parameters.add(value)
        }

        sql.append(')')
    }
}

private data class ConflictNode(val columns: List<ColumnRef>) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("on conflict (")

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql.append(") do")
    }
}

private data class UpdateNode(
    val table: TableRef?,
    val set: List<Pair<ColumnRef, Value>>,
    val condition: Condition?,
) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append("update ")

        if (table != null) {
            sql
                .append(table)
                .append(' ')
        }

        sql.append("set ")

        for ((index, value) in set.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(value.first.name)
            sql.append(" = ")
            value.second.generate(sql, parameters, named)
        }

        if (condition != null) {
            sql.append(" where ")

            condition.generate(sql, parameters)
        }
    }
}

private data class StaticNode(val value: String) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>, named: MutableMap<String, Int>) {
        sql.append(value)
    }
}

sealed interface Condition {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

private data class ConditionEq(
    val left: ColumnRef,
    val right: Any?,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" = ?")
        parameters.add(right)
    }
}

private data class ConditionNe(
    val left: ColumnRef,
    val right: Any?,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" <> ?")
        parameters.add(right)
    }
}

private data class ConditionLt(
    val left: ColumnRef,
    val right: Any?,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" < ?")
        parameters.add(right)
    }
}

private data class ConditionLe(
    val left: ColumnRef,
    val right: Any?,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" <= ?")
        parameters.add(right)
    }
}

private data class ConditionGt(
    val left: ColumnRef,
    val right: Any?
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" > ?")
        parameters.add(right)
    }
}

private data class ConditionGe(
    val left: ColumnRef,
    val right: Any?,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append(left)
            .append(" >= ?")
        parameters.add(right)
    }
}

private data class ConditionAnd(
    val left: Condition,
    val right: Condition,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        left.generate(sql, parameters)
        sql.append(" and ")
        right.generate(sql, parameters)
    }
}

private data class ConditionOr(
    val left: Condition,
    val right: Condition,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        left.generate(sql, parameters)
        sql.append(" or ")
        right.generate(sql, parameters)
    }
}

private data class ConditionNot(
    val condition: Condition,
) : Condition {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("not ")
        condition.generate(sql, parameters)
    }
}

sealed interface Constraint {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

private data class ConstraintPrimaryKey(
    val columns: List<ColumnRef>,
) : Constraint {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("primary key (")

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql.append(')')
    }
}

private data class ConstraintForeignKey(
    val pColumns: List<ColumnRef>,
    val fTable: TableRef,
    val fColumns: List<ColumnRef>,
) : Constraint {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("foreign key (")

        for ((index, column) in pColumns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql
            .append(") references ")
            .append(fTable)
            .append(" (")

        for ((index, column) in fColumns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql.append(')')
    }
}

private data class ConstraintUnique(
    val columns: List<ColumnRef>,
) : Constraint {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("unique (")

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql.append(')')
    }
}

data class Statement(
    val statement: PreparedStatement,
    val named: Map<String, Int>,
) : AutoCloseable {

    override fun close() {
        statement.close()
    }
}

class SQL(val connection: Connection) {

    private val nodes = mutableListOf<Node>()
    private var sealed = false

    private fun add(node: Node): SQL {
        if (sealed) throw IllegalStateException()
        nodes.add(node)
        return this
    }

    fun create(
        table: TableRef,
        modifiers: List<CreateTableMod> = emptyList(),
        columns: List<ColumnDef> = emptyList(),
        constraints: List<ConstraintDef> = emptyList(),
    ): SQL {
        return add(CreateNode(modifiers, table, columns, constraints))
    }

    fun alter(table: TableRef): SQL {
        return add(AlterNode(table))
    }

    fun add(name: String, constraint: Constraint): SQL {
        return add(AddNode(name, constraint))
    }

    fun delete(): SQL {
        return add(StaticNode("delete"))
    }

    fun select(vararg columns: ColumnRef): SQL {
        return add(SelectNode(columns.asList()))
    }

    fun from(table: TableRef): SQL {
        return add(FromNode(table))
    }

    fun where(condition: Condition): SQL {
        return add(WhereNode(condition))
    }

    fun order(column: ColumnRef, order: Order): SQL {
        return add(OrderNode(column, order))
    }

    fun limit(limit: Int): SQL {
        return add(LimitNode(limit))
    }

    fun insert(table: TableRef, columns: List<ColumnRef>, values: List<Any?>): SQL {
        return add(InsertNode(table, columns, values))
    }

    fun conflict(columns: List<ColumnRef>, next: (SQL) -> SQL): SQL {
        return next(add(ConflictNode(columns)))
    }

    fun update(vararg set: Pair<ColumnRef, Value>): SQL {
        return add(UpdateNode(null, set.asList(), null))
    }

    fun update(table: TableRef, set: List<Pair<ColumnRef, Value>>, condition: Condition): SQL {
        return add(UpdateNode(table, set, condition))
    }

    fun prepare(): Statement {
        sealed = true

        val sql = StringBuilder()
        val parameters = mutableListOf<Any?>()
        val named = mutableMapOf<String, Int>()

        for ((index, node) in nodes.withIndex()) {
            if (index > 0) sql.append(' ')
            node.generate(sql, parameters, named)
        }

        val statement = connection.prepareStatement(sql.toString())

        for ((index, value) in parameters.withIndex()) {
            statement.setObject(index + 1, value)
        }

        return Statement(statement, named)
    }

    fun execute() = prepare().use { it.statement.execute() }
}

inline fun <reified T : Any> SQL.create(): SQL {
    val table = tableOf<T>()
    val columns = columns<T>()

    val columnDefs = mutableListOf<ColumnDef>()
    val constraintDefs = mutableListOf<ConstraintDef>()

    for ((name, type, parameter) in columns) {
        val primaryKey = parameter.findAnnotation<PrimaryKey>()
        val foreignKey = parameter.findAnnotation<ForeignKey>()
        val unique = parameter.findAnnotation<Unique>()

        val jdbcType = when (type) {
            Boolean::class -> JDBCType.BOOLEAN
            Byte::class -> JDBCType.TINYINT
            Short::class -> JDBCType.SMALLINT
            Int::class -> JDBCType.INTEGER
            Long::class -> JDBCType.BIGINT
            Float::class -> JDBCType.FLOAT
            Double::class -> JDBCType.DOUBLE
            String::class -> JDBCType.VARCHAR
            Date::class -> JDBCType.DATE
            Time::class -> JDBCType.TIME
            Timestamp::class -> JDBCType.TIMESTAMP
            else -> throw Error("no jdbc type for '$type'")
        }

        val notNull = !parameter.type.isMarkedNullable

        columnDefs += ColumnDef(name, jdbcType.name, notNull)

        val ref = ColumnRef(table, name)

        if (primaryKey != null) {
            constraintDefs += define(primaryKey(ref))
        }

        if (foreignKey != null) {
            val fTable = TableRef(foreignKey.table)
            val fColumn = ColumnRef(fTable, foreignKey.column)

            constraintDefs += define(foreignKey(listOf(ref), fTable, listOf(fColumn)))
        }

        if (unique != null) {
            constraintDefs += define(unique(ref))
        }
    }

    return create(
        table,
        listOf(CreateTableMod.IF_NOT_EXISTS),
        columnDefs,
        constraintDefs,
    )
}

inline fun <reified T : Any> SQL.select(): SQL {
    val table = tableOf<T>()
    val columns = columns<T>()

    return select(*columns.map { (name) -> ColumnRef(table, name) }.toTypedArray()).from(table)
}

inline fun <reified T : Any> SQL.delete(): SQL {
    val table = tableOf<T>()

    return delete().from(table)
}

inline fun <reified T : Any> SQL.insert(): SQL {
    val table = tableOf<T>()
    val columns = columns<T>()

    return insert(
        table,
        columns.map { (name) -> ColumnRef(table, name) },
        columns.map { null },
    )
}

inline fun <reified T : Any> SQL.insert(value: T): SQL {
    val table = tableOf<T>()
    val columns = columns<T>()

    return insert(
        table,
        columns.map { (name) -> ColumnRef(table, name) },
        columns.map { (name, type, parameter, property) ->
            // TODO: convert to jdbc type
            property.call(value)
        },
    )
}

inline fun <reified T : Any> SQL.conflict(property: KProperty1<T, *>, noinline next: (SQL) -> SQL): SQL {
    val column = columnOf<T>(property)

    return conflict(listOf(column), next)
}

inline fun <reified T : Any> SQL.update(vararg set: Pair<KProperty1<T, *>, Value>): SQL {
    return update(*set.map { (property, value) -> columnOf<T>(property) to value }.toTypedArray())
}

inline fun <reified T : Any> SQL.query(): List<T> {
    val constructor = T::class.primaryConstructor
        ?: throw UnsupportedOperationException()

    val columns = columns<T>()

    return prepare().use { (statement) ->
        statement.executeQuery().use { result ->
            val entities = mutableListOf<T>()
            while (result.next()) {
                val arguments = columns.map { (name, type, parameter) ->
                    // TODO: convert to parameter type
                    result.getObject(name, type.java)
                }.toTypedArray()

                entities += constructor.call(*arguments)
            }
            entities
        }
    }
}

inline fun <reified T : Any> SQL.batch(noinline callback: ((T) -> Unit) -> Unit) {
    val table = tableOf<T>()
    val columns = columns<T>()

    prepare().use { (statement, named) ->
        callback { instance ->
            for ((name, type, parameter, property) in columns) {
                val index = named["$table.$name"] ?: -1
                val value = property.call(instance)

                // TODO: convert to jdbc type
                statement.setObject(index + 1, value)
            }

            statement.addBatch()
        }

        statement.executeLargeBatch()
    }
}

inline fun <reified T : Any> tableOf(): TableRef {
    val table = T::class.findAnnotation<Table>()
        ?: throw UnsupportedOperationException()

    return TableRef(table.value)
}

inline fun <reified T : Any> columnOf(property: KProperty1<T, *>): ColumnRef {
    val constructor = T::class.primaryConstructor
        ?: throw UnsupportedOperationException()

    val parameter = constructor.parameters.first { it.name == property.name }
    val column = parameter.findAnnotation<Column>()!!

    return ColumnRef(
        tableOf<T>(),
        column.value.ifEmpty { parameter.name!! },
    )
}

data class ColumnData<T : Any>(
    val name: String,
    val type: KClass<*>,
    val parameter: KParameter,
    val property: KProperty1<T, *>,
)

inline fun <reified T : Any> columns(): List<ColumnData<T>> {
    val constructor = T::class.primaryConstructor
        ?: throw UnsupportedOperationException()
    val properties = T::class.memberProperties

    return constructor.parameters
        .map { it.findAnnotation<Column>()!! to it }
        .map { (column, parameter) ->
            ColumnData(
                column.value.ifEmpty { parameter.name!! },
                if (column.type == Unit::class)
                    parameter.type.classifier as KClass<*>
                else column.type,
                parameter,
                properties.first { it.name == parameter.name },
            )
        }
}

fun excluded(name: String): ColumnRef = ColumnRef(TableRef("excluded"), name)

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.eq(value: V): Condition =
    columnOf<T>(this) eq value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.ne(value: V): Condition =
    columnOf<T>(this) ne value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.lt(value: V): Condition =
    columnOf<T>(this) lt value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.le(value: V): Condition =
    columnOf<T>(this) le value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.gt(value: V): Condition =
    columnOf<T>(this) gt value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.ge(value: V): Condition =
    columnOf<T>(this) ge value

infix fun ColumnRef.eq(value: Any?): Condition = ConditionEq(this, value)
infix fun ColumnRef.ne(value: Any?): Condition = ConditionNe(this, value)
infix fun ColumnRef.lt(value: Any?): Condition = ConditionLt(this, value)
infix fun ColumnRef.le(value: Any?): Condition = ConditionLe(this, value)
infix fun ColumnRef.gt(value: Any?): Condition = ConditionGt(this, value)
infix fun ColumnRef.ge(value: Any?): Condition = ConditionGe(this, value)

infix fun Condition.and(other: Condition): Condition = ConditionAnd(this, other)
infix fun Condition.or(other: Condition): Condition = ConditionOr(this, other)

operator fun Condition.not(): Condition =
    if (this is ConditionNot)
        condition
    else ConditionNot(this)

fun primaryKey(vararg columns: ColumnRef): Constraint =
    ConstraintPrimaryKey(columns.asList())

fun foreignKey(pColumns: List<ColumnRef>, fTable: TableRef, fColumns: List<ColumnRef>): Constraint =
    ConstraintForeignKey(pColumns, fTable, fColumns)

fun unique(vararg columns: ColumnRef): Constraint = ConstraintUnique(columns.asList())

fun define(constraint: Constraint): ConstraintDef = ConstraintDef(null, constraint)
fun define(name: String, constraint: Constraint): ConstraintDef = ConstraintDef(name, constraint)
