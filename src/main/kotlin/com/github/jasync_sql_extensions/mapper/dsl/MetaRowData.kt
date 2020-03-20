package com.github.jasync_sql_extensions.mapper.dsl

import com.github.jasync.sql.db.RowData

class MetaRowData(var rowData: RowData?, val columns: List<String>, val additionals: Array<List<Any>>) : RowData {
    override val size: Int get() = columns.size + additionals.size
    override fun contains(element: Any?): Boolean =
        if (rowData!!.contains(element)) true else additionals.contains(element)

    override fun containsAll(elements: Collection<Any?>): Boolean = elements.all { contains(it) }

    override operator fun get(index: Int): Any? = if (index < rowData!!.size) rowData!![index] else additionals[index - rowData!!.size]
    override fun get(column: String): Any? = rowData!![column] ?: additionals[columns.indexOf(column)]
    override fun indexOf(element: Any?): Int = columns.indexOf(element)
    override fun isEmpty(): Boolean = columns.isEmpty()

    override fun iterator(): Iterator<Any?> = object : Iterator<Any?> {
        private var i: Int = -1

        override fun hasNext(): Boolean = i < size
        override fun next(): Any? = get(i++)
    }

    override fun lastIndexOf(element: Any?): Int {
        val i = additionals.lastIndexOf(element)
        return if (i == -1) additionals.indexOf(element) else i
    }

    override fun listIterator(): ListIterator<Any?> = this.listIterator(0)

    override fun listIterator(index: Int): ListIterator<Any?> = object : ListIterator<Any?> {
        private var i: Int = index

        override fun hasNext(): Boolean = i < size
        override fun next(): Any? {
            val e = get(i)
            i++
            return e
        }

        override fun previous(): Any? = get(i--)
        override fun hasPrevious(): Boolean = i > 0
        override fun nextIndex(): Int = i
        override fun previousIndex(): Int = i - 1
    }

    override fun rowNumber(): Int = rowData!!.rowNumber()

    override fun subList(fromIndex: Int, toIndex: Int): List<Any?> =
        listOf(rowData, additionals).subList(fromIndex, toIndex)
}
