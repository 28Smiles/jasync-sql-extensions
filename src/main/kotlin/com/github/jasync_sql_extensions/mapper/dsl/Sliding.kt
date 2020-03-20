package com.github.jasync_sql_extensions.mapper.dsl

import com.github.jasync.sql.db.RowData

interface Sliding<T : Any> {
    fun hasNext(): Boolean
    fun next(ignoreIndex: Boolean = false): T?
    fun all(): List<T> {
        val collector: MutableList<T> = mutableListOf()

        while (this.hasNext()) {
            val element = this.next()
            if (element != null) {
                collector.add(element)
            }
        }

        return collector
    }
}

class IteratingSlider<T : Any>(val parent: Iterator<T>): Sliding<T> {
    override fun next(ignoreIndex: Boolean): T? = parent.next()
    override fun hasNext(): Boolean = parent.hasNext()
}

class SlidingWindow(val parent: Iterator<RowData>, val index: Int): Sliding<RowData> {
    private var currentRow: RowData? = if (parent.hasNext()) parent.next() else null
    private var nextRow: RowData? = if (parent.hasNext()) parent.next() else null

    override fun hasNext(): Boolean = currentRow != null
    override fun next(ignoreIndex: Boolean): RowData? {
        val result: RowData? = if (ignoreIndex || nextRow == null || currentRow!![index] != nextRow!![index]) currentRow else null
        currentRow = nextRow
        nextRow = if (parent.hasNext()) parent.next() else null

        return result
    }
}
