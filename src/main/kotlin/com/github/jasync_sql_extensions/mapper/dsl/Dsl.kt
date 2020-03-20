package com.github.jasync_sql_extensions.mapper.dsl

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.mapTo
import com.github.jasync_sql_extensions.toSnakeCased
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@DslMarker
annotation class MappingDslMarker

@MappingDslMarker
data class Projection<T : Any>(
    private val columns: List<String>,
    private val beanClass: KClass<T>,
    private var prefix: String = "",
    private var column: KProperty1<T, Any>? = null,
    private var skipNull: Boolean = false,
    private var mappingMeta: MappingMeta<T>? = null
) {
    fun distinct(column: KProperty1<T, Any>, skipNull: Boolean = false): Projection<T> {
        this.column = column
        this.skipNull = skipNull

        return this
    }

    fun prefix(prefix: String): Projection<T> {
        this.prefix = prefix

        return this
    }

    fun map(m: MappingMeta<T>.() -> Unit): Projection<T> {
        this.mappingMeta = MappingMeta(columns)
        m(this.mappingMeta!!)

        return this
    }

    fun projector(): Projector<T> = Projector(
        columns,
        beanClass,
        prefix,
        column,
        skipNull,
        mappingMeta ?: MappingMeta(columns)
    )
}

@MappingDslMarker
data class MappingMeta<T : Any>(
    val columns: List<String>
) {
    val groups: MutableMap<KProperty1<T, *>, Projector<out Any>> = mutableMapOf()

    inline fun <reified Q : Any, V : List<Q>> group(
        property: KProperty1<T, V>,
        project: Projection<Q>.() -> Unit
    ): Projection<Q> {
        val projection: Projection<Q> = Projection(
            columns,
            Q::class
        ).apply(project)
        this.groups[property] = projection.projector()

        return projection
    }

    inline fun <reified Q : Any, V : Q?> one(
        property: KProperty1<T, V>,
        project: Projection<Q>.() -> Unit
    ): Projection<Q> {
        val projection: Projection<Q> = Projection(
            columns,
            Q::class
        ).apply(project)
        this.groups[property] = projection.projector()

        return projection
    }
}

inline fun <reified T: Any> ResultSet.project(dsl: Projection<T>.() -> Unit): List<T>
     = Projection(
    this.columnNames(),
    T::class
).apply(dsl).projector().iterator(this).all()
