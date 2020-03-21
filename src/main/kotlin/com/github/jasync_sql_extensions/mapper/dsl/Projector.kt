package com.github.jasync_sql_extensions.mapper.dsl

import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.mapTo
import com.github.jasync_sql_extensions.toSnakeCased
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

data class Projector<T : Any>(
    private val columns: List<String>,
    private val beanClass: KClass<T>,
    private var prefix: String,
    private var column: KProperty1<T, Any>?,
    private var skipNull: Boolean,
    private var mappingMeta: MappingMeta<T>
) {
    private val prefixedIndex: Int? = column?.let { columns.indexOf(prefix + it.name.toSnakeCased()) }
    private val specials: Set<String> = mappingMeta.groups.keys.map { it.name }.toSet()
    private val allColumns = columns + mappingMeta.groups.keys.map { property ->
        this.prefix + property.name.toSnakeCased()
    }
    private val mapper = iteratorToMapper<RowData, T> { iterator ->
        iterator.mapTo(
            allColumns,
            beanClass,
            prefix,
            specials
        )
    }

    fun iterator(rows: List<RowData>): Sliding<T> {
        val projectors: List<Sliding<out Any>> = mappingMeta.groups.values.map { it.iterator(rows) }
        val slider: Sliding<RowData> = prefixedIndex?.let {
            SlidingWindow(
                rows.iterator(),
                it
            )
        } ?: IteratingSlider(rows.iterator())
        val collected: Array<MutableList<Any>> = Array(projectors.size) { mutableListOf<Any>() }
        val metaRowData = MetaRowData(
            null,
            columns,
            collected as Array<List<Any>>
        )

        return object : Sliding<T> {
            override fun hasNext(): Boolean = slider.hasNext()

            override fun next(ignoreIndex: Boolean): T? {
                val nextRow = slider.next(ignoreIndex)

                return if (nextRow != null && prefixedIndex?.let { nextRow[it] != null } != false) {
                    for (i in collected.indices) {
                        projectors[i].next(ignoreIndex = true)?.let { value ->
                            collected[i].add(value)
                        }
                    }

                    metaRowData.rowData = nextRow
                    val mapped = mapper(metaRowData)

                    for (i in collected.indices) {
                        collected[i] = mutableListOf()
                    }

                    mapped
                } else {
                    for (i in collected.indices) {
                        projectors[i].next(ignoreIndex = ignoreIndex)?.let { value ->
                            collected[i].add(value)
                        }
                    }

                    null
                }
            }
        }
    }
}
