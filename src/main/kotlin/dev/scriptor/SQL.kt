package dev.scriptor

import dev.scriptor.annotation.*
import java.nio.file.Path
import java.sql.Connection
import java.sql.JDBCType
import java.sql.PreparedStatement
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType
import kotlin.time.Instant
import kotlin.uuid.Uuid

sealed interface Value {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

sealed interface Node {
    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

enum class Order { ASC, DESC }

data class TableRef(val name: String) {
    override fun toString(): String = name
}

data class ColumnRef(val table: TableRef, val name: String) : Value {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("alter table ")
            .append(table)
    }
}

private data class AddNode(val name: String?, val constraint: Constraint) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("select ")

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column)
        }
    }
}

private data class FromNode(val table: TableRef) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("from ")
            .append(table)
    }
}

private data class WhereNode(val condition: Condition) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("where ")

        condition.generate(sql, parameters)
    }
}

private data class OrderNode(val column: ColumnRef, val order: Order) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("order by ")
        sql.append(column)
        sql.append(' ').append(order)
    }
}

private data class LimitNode(val limit: Int) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("limit ?")

        parameters.add(limit)
    }
}

private data class InsertNode(
    val table: TableRef,
    val columns: List<ColumnRef>,
    val values: List<Any?>,
) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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

        for ((index, _) in values.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append('?')
        }

        sql.append(')')

        parameters.addAll(values)
    }
}

private data class ConflictNode(val columns: List<ColumnRef>) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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
            value.second.generate(sql, parameters)
        }

        if (condition != null) {
            sql.append(" where ")

            condition.generate(sql, parameters)
        }
    }
}

private data class StaticNode(val value: String) : Node {
    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
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

class SQL(val connection: Connection) {

    private val nodes = mutableListOf<Node>()

    fun create(
        table: TableRef,
        modifiers: List<CreateTableMod> = emptyList(),
        columns: List<ColumnDef> = emptyList(),
        constraints: List<ConstraintDef> = emptyList(),
    ): SQL {
        nodes += CreateNode(modifiers, table, columns, constraints)
        return this
    }

    fun alter(table: TableRef): SQL {
        nodes += AlterNode(table)
        return this
    }

    fun add(name: String, constraint: Constraint): SQL {
        nodes += AddNode(name, constraint)
        return this
    }

    fun delete(): SQL {
        nodes += StaticNode("delete")
        return this
    }

    fun select(vararg columns: ColumnRef): SQL {
        nodes += SelectNode(columns.asList())
        return this
    }

    fun from(table: TableRef): SQL {
        nodes += FromNode(table)
        return this
    }

    fun where(condition: Condition): SQL {
        nodes += WhereNode(condition)
        return this
    }

    fun order(column: ColumnRef, order: Order): SQL {
        nodes += OrderNode(column, order)
        return this
    }

    fun limit(limit: Int): SQL {
        nodes += LimitNode(limit)
        return this
    }

    fun insert(table: TableRef, columns: List<ColumnRef>, values: List<Any?>): SQL {
        nodes += InsertNode(table, columns, values)
        return this
    }

    fun conflict(columns: List<ColumnRef>, next: (SQL) -> SQL): SQL {
        nodes += ConflictNode(columns)
        return next(this)
    }

    fun update(vararg set: Pair<ColumnRef, Value>): SQL {
        nodes += UpdateNode(null, set.asList(), null)
        return this
    }

    fun update(table: TableRef, set: List<Pair<ColumnRef, Value>>, condition: Condition): SQL {
        nodes += UpdateNode(table, set, condition)
        return this
    }

    fun prepare(): PreparedStatement {
        val sql = StringBuilder()
        val parameters = mutableListOf<Any?>()

        for ((index, node) in nodes.withIndex()) {
            if (index > 0) sql.append(' ')
            node.generate(sql, parameters)
        }

        val statement = connection.prepareStatement(sql.toString())

        for ((index, value) in parameters.withIndex()) {
            statement.setObject(index + 1, value)
        }

        return statement
    }

    fun execute() = prepare().use { it.execute() }
}

inline fun <reified T : Any> SQL.create(): SQL {
    val klass = T::class

    val tableRef = tableOf(klass)
    val columnDefs = mutableListOf<ColumnDef>()
    val constraintDefs = mutableListOf<ConstraintDef>()

    val typeMap = connection.typeMap.entries.associate { (key, value) -> value to key }

    val columns = columns(klass)

    for ((name, property) in columns) {
        val primaryKey = property.findAnnotation<PrimaryKey>()
        val foreignKey = property.findAnnotation<ForeignKey>()
        val unique = property.findAnnotation<Unique>()

        val type = property.returnType

        val sqlType = typeMap[type.javaType] ?: when (type.withNullability(false).classifier) {
            String::class, Uuid::class, Path::class -> JDBCType.VARCHAR.name
            Instant::class -> JDBCType.TIMESTAMP.name
            Long::class -> JDBCType.BIGINT.name
            else -> throw Error("sql of $type")
        }

        val notNull = !type.isMarkedNullable

        columnDefs += ColumnDef(name, sqlType, notNull)

        val ref = ColumnRef(tableRef, name)

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
        tableRef,
        listOf(CreateTableMod.IF_NOT_EXISTS),
        columnDefs,
        constraintDefs,
    )
}

inline fun <reified T : Any> SQL.select(): SQL {
    val klass = T::class

    val tableRef = tableOf(klass)
    val columnRefs = columns(klass)
        .map { ColumnRef(tableRef, it.first) }
        .toTypedArray()

    return select(*columnRefs).from(tableRef)
}

inline fun <reified T : Any> SQL.delete(): SQL {
    val klass = T::class

    val tableRef = tableOf(klass)

    return delete().from(tableRef)
}

inline fun <reified T : Any> SQL.insert(): SQL {
    val klass = T::class

    val columns = columns(klass)

    val tableRef = tableOf(klass)
    val columnRefs = columns.map { ColumnRef(tableRef, it.first) }
    val values = columns.map { null }

    return insert(tableRef, columnRefs, values)
}

inline fun <reified T : Any> SQL.insert(value: T): SQL {
    val klass = T::class

    val columns = columns(klass)

    val tableRef = tableOf(klass)
    val columnRefs = columns.map { ColumnRef(tableRef, it.first) }
    val values = columns.map { it.second.call(value) }

    return insert(tableRef, columnRefs, values)
}

inline fun <reified T : Any, reified V> SQL.conflict(property: KProperty1<T, V>, noinline next: (SQL) -> SQL): SQL {
    val klass = T::class

    val columnRef = columnOf(klass, property)

    return conflict(listOf(columnRef), next)
}

inline fun <reified T : Any> SQL.query(): List<T> {
    val klass = T::class

    klass.findAnnotation<Table>() ?: throw UnsupportedOperationException()

    val columns = columns(klass)

    return prepare().use { statement ->
        statement.executeQuery().use { result ->
            val entities = mutableListOf<T>()
            while (result.next()) {
                val entity = klass.constructors
                    .single { it.parameters.all(KParameter::isOptional) }
                    .call()

                for ((name, property) in columns) {
                    property.setter.call(
                        entity,
                        result.getObject(
                            name,
                            (property.returnType.classifier as? KClass<*>?)?.java,
                        )
                    )
                }

                entities += entity
            }
            entities
        }
    }
}

inline fun <reified T : Any> SQL.batch(noinline callback: ((T) -> Unit) -> Unit) {
    val klass = T::class

    val columns = columns(klass)

    prepare().use { statement ->
        callback {
            for ((index, entry) in columns.withIndex()) {
                val value = entry.second.call(it)
                statement.setObject(index + 1, value)
            }

            statement.addBatch()
        }

        statement.executeLargeBatch()
    }
}

fun <T : Any> tableOf(klass: KClass<T>): TableRef {
    val table = klass.findAnnotation<Table>() ?: throw UnsupportedOperationException()
    return TableRef(table.value)
}

fun <T : Any, V> columnOf(klass: KClass<T>, property: KProperty1<T, V>): ColumnRef {
    val column = property.findAnnotation<Column>() ?: throw UnsupportedOperationException()
    return ColumnRef(tableOf(klass), column.value)
}

fun <T : Any> columns(klass: KClass<T>): List<Pair<String, KMutableProperty<*>>> {
    return klass.members
        .filter { it.hasAnnotation<Column>() }
        .filterIsInstance<KMutableProperty<*>>()
        .map { Pair(it.findAnnotation<Column>()!!.value, it) }
        .sortedWith { (a, _), (b, _) -> a.compareTo(b) }
}

fun excluded(name: String): ColumnRef = ColumnRef(TableRef("excluded"), name)

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.eq(value: V): Condition =
    columnOf(T::class, this) eq value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.ne(value: V): Condition =
    columnOf(T::class, this) ne value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.lt(value: V): Condition =
    columnOf(T::class, this) lt value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.le(value: V): Condition =
    columnOf(T::class, this) le value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.gt(value: V): Condition =
    columnOf(T::class, this) gt value

inline infix fun <reified T : Any, reified V> KProperty1<T, V>.ge(value: V): Condition =
    columnOf(T::class, this) ge value

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
