package dev.scriptor

import dev.scriptor.annotation.Column
import dev.scriptor.annotation.ForeignKey
import dev.scriptor.annotation.PrimaryKey
import dev.scriptor.annotation.Table
import java.nio.file.Path
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.time.Instant
import kotlin.uuid.Uuid

sealed interface Node {

    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

enum class Order { ASC, DESC }

data class TableRef(val name: String) {

    override fun toString(): String = name
}

data class ColumnRef(val table: TableRef, val name: String) {

    override fun toString(): String = "$table.$name"
}

data class ColumnDef(
    val name: String,
    val type: String,
    val notNull: Boolean = false,
) {

    override fun toString(): String = "$name $type ${if (notNull) "not null" else ""}"
}

data class ConstraintDef(
    val name: String? = null,
    val constraint: Constraint,
) {
    constructor(constraint: Constraint) : this(null, constraint)

    override fun toString(): String = if (name != null) "constraint $name $constraint" else "$constraint"
}

enum class CreateTableMod(val value: String) {
    IF_NOT_EXISTS("if not exists");

    override fun toString(): String = value
}

private data class CreateTableNode(
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
                sql.append(constraint)
            }
            sql.append(')')
        }
    }
}

private data class AlterTableNode(val table: TableRef) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("alter table ")
            .append(table)
    }
}

private data class AddConstraintNode(val name: String, val constraint: Constraint) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("add constraint ")
            .append(name)
            .append(' ')
            .append(constraint)
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

private data class OrderByNode(val column: ColumnRef, val order: Order) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("order by ")
            .append(column)
            .append(' ')
            .append(order)
    }
}

private data class LimitNode(val limit: Int) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("limit ?")

        parameters.add(limit)
    }
}

private data class IntoNode(val table: TableRef) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql
            .append("into ")
            .append(table)
    }
}

private data class ValuesNode(val values: List<Any?>) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("values (")
        for ((index, _) in values.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append('?')
        }
        sql.append(')')

        parameters.addAll(values)
    }
}

private data class StaticNode(val value: String) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(value)
    }
}

private data class ColumnRefNode(val columns: List<ColumnRef>) : Node {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append('(')

        for ((index, column) in columns.withIndex()) {
            if (index > 0) sql.append(", ")
            sql.append(column.name)
        }

        sql.append(')')
    }
}

sealed interface Condition {

    fun generate(sql: StringBuilder, parameters: MutableList<Any?>)
}

private data class ConditionEq(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" = ?")

        parameters.add(value)
    }
}

private data class ConditionNe(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" <> ?")

        parameters.add(value)
    }
}

private data class ConditionLt(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" < ?")

        parameters.add(value)
    }
}

private data class ConditionLe(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" <= ?")

        parameters.add(value)
    }
}

private data class ConditionGt(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" > ?")

        parameters.add(value)
    }
}

private data class ConditionGe(val column: ColumnRef, val value: Any?) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append(column).append(" >= ?")

        parameters.add(value)
    }
}

private data class ConditionAnd(val left: Condition, val right: Condition) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        left.generate(sql, parameters)
        sql.append(" and ")
        right.generate(sql, parameters)
    }
}

private data class ConditionOr(val left: Condition, val right: Condition) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        left.generate(sql, parameters)
        sql.append(" or ")
        right.generate(sql, parameters)
    }
}

private data class ConditionNot(val condition: Condition) : Condition {

    override fun generate(sql: StringBuilder, parameters: MutableList<Any?>) {
        sql.append("not ")
        condition.generate(sql, parameters)
    }
}

sealed interface Constraint

private data class ConstraintPrimaryKey(val columns: List<String>) : Constraint {

    override fun toString(): String = "primary key (${columns.joinToString(", ")})"
}

private data class ConstraintForeignKey(
    val key: String,
    val references: String,
    val columns: List<String>,
) : Constraint {

    override fun toString(): String =
        "foreign key (${key}) references ${references}(${columns.joinToString(", ")})"
}

private data class ConstraintUnique(val columns: List<String>) : Constraint {

    override fun toString(): String = "unique (${columns.joinToString(", ")})"
}

class SQL {

    private val nodes = mutableListOf<Node>()

    fun createTable(
        table: TableRef,
        modifiers: List<CreateTableMod> = emptyList(),
        columns: List<ColumnDef> = emptyList(),
        constraints: List<ConstraintDef> = emptyList(),
    ): SQL {
        nodes += CreateTableNode(modifiers, table, columns, constraints)
        return this
    }

    fun alterTable(table: TableRef): SQL {
        nodes += AlterTableNode(table)
        return this
    }

    fun addConstraint(name: String, constraint: Constraint): SQL {
        nodes += AddConstraintNode(name, constraint)
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

    fun orderBy(column: ColumnRef, order: Order): SQL {
        nodes += OrderByNode(column, order)
        return this
    }

    fun limit(limit: Int): SQL {
        nodes += LimitNode(limit)
        return this
    }

    fun insertInto(table: TableRef): SQL {
        nodes += IntoNode(table)
        return this
    }

    fun values(vararg values: Any?): SQL {
        nodes += ValuesNode(values.asList())
        return this
    }

    operator fun invoke(vararg columns: ColumnRef): SQL {
        nodes += ColumnRefNode(columns.asList())
        return this
    }

    fun prepare(connection: Connection): PreparedStatement {
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

    fun execute(connection: Connection) {
        prepare(connection).use { statement -> statement.execute() }
    }
}

inline fun <reified T : Any> SQL.createTable(): SQL {
    val klass = T::class

    val table = tableOf(klass)
    val columns = mutableListOf<ColumnDef>()
    val constraints = mutableListOf<ConstraintDef>()

    for (member in klass.members) {
        if (member !is KProperty) continue

        val column = member.findAnnotation<Column>()
        if (column == null) continue

        val primaryKey = member.findAnnotation<PrimaryKey>()
        val foreignKey = member.findAnnotation<ForeignKey>()

        val name = column.value
        val type = member.getter.returnType

        val sqlType = when {
            type.withNullability(false).isSubtypeOf(Uuid::class.starProjectedType)
                -> "uuid"

            type.withNullability(false).isSubtypeOf(CharSequence::class.starProjectedType) ||
                    type.withNullability(false).isSubtypeOf(Enum::class.starProjectedType) ||
                    type.withNullability(false).isSubtypeOf(Path::class.starProjectedType)
                -> "string"

            type.withNullability(false).isSubtypeOf(Byte::class.starProjectedType)
                -> "byte"

            type.withNullability(false).isSubtypeOf(Short::class.starProjectedType)
                -> "short"

            type.withNullability(false).isSubtypeOf(Int::class.starProjectedType)
                -> "int"

            type.withNullability(false).isSubtypeOf(Long::class.starProjectedType)
                -> "long"

            type.withNullability(false).isSubtypeOf(Instant::class.starProjectedType)
                -> "timestamp"

            else -> throw UnsupportedOperationException("sql of ${type.withNullability(false)}")
        }

        val notNull = !type.isMarkedNullable

        columns += ColumnDef(name, sqlType, notNull)

        if (primaryKey != null) {
            constraints += ConstraintDef(primaryKey(name))
        }

        if (foreignKey != null) {
            constraints += ConstraintDef(foreignKey(name, foreignKey.table, foreignKey.column))
        }
    }

    return createTable(
        table,
        listOf(CreateTableMod.IF_NOT_EXISTS),
        columns,
        constraints,
    )
}

inline fun <reified T : Any> SQL.selectFrom(): SQL {
    val klass = T::class

    val tableRef = tableOf(klass)

    val columnRefs = klass.members
        .mapNotNull { it.findAnnotation<Column>() }
        .map { ColumnRef(tableRef, it.value) }
        .toTypedArray()

    return this
        .select(*columnRefs)
        .from(tableRef)
}

inline fun <reified T : Any> SQL.deleteFrom(): SQL {
    val klass = T::class

    val tableRef = tableOf(klass)

    return this
        .delete()
        .from(tableRef)
}

inline fun <reified T : Any> SQL.insertInto(): SQL {
    val klass = T::class

    val columns = klass.members
        .mapNotNull {
            val column = it.findAnnotation<Column>()
            if (column != null && it is KProperty)
                Pair(column.value, it)
            else null
        }
        .associate { it.first to it.second }

    val tableRef = tableOf(klass)
    val columnRefs = columns
        .map { ColumnRef(tableRef, it.key) }
        .toTypedArray()

    return insertInto(tableRef)(*columnRefs)
}

inline fun <reified T : Any> SQL.insertValues(connection: Connection, entity: T) {
    val klass = T::class

    val columns = klass.members
        .mapNotNull {
            val column = it.findAnnotation<Column>()
            if (column != null && it is KProperty)
                Pair(column.value, it)
            else null
        }
        .associate { it.first to it.second }

    val tableRef = tableOf(klass)
    val columnRefs = columns
        .map { ColumnRef(tableRef, it.key) }
        .toTypedArray()

    val values = columns
        .map { it.value.getter.call(entity) }
        .toTypedArray()

    insertInto(tableRef)(*columnRefs)
        .values(*values)
        .prepare(connection)
        .use { statement -> statement.executeUpdate() }
}

inline fun <reified T> SQL.query(connection: Connection): List<T> {

    val klass = T::class
    klass.findAnnotation<Table>() ?: throw UnsupportedOperationException()
    val columns = klass.members
        .mapNotNull {
            val column = it.findAnnotation<Column>()
            if (column != null)
                Pair(column.value, it)
            else null
        }
        .associate { it.first to it.second }

    return prepare(connection).use { statement ->
        statement.executeQuery().use { result ->
            val entities = mutableListOf<T>()
            while (result.next()) {
                val entity = klass.constructors
                    .single { it.parameters.all(KParameter::isOptional) }
                    .call()

                for ((name, callee) in columns) {
                    if (callee is KMutableProperty) {
                        callee.setter.call(
                            entity,
                            result.getObject(
                                name,
                                (callee.getter.returnType.classifier as? KClass<*>?)?.java,
                            )
                        )
                    }
                }

                entities += entity
            }
            entities
        }
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

fun Condition.not(): Condition = if (this is ConditionNot) condition else ConditionNot(this)

fun primaryKey(vararg columns: String): Constraint =
    ConstraintPrimaryKey(columns.asList())

fun foreignKey(key: String, references: String, vararg columns: String): Constraint =
    ConstraintForeignKey(key, references, columns.asList())

fun unique(vararg columns: String): Constraint = ConstraintUnique(columns.asList())
