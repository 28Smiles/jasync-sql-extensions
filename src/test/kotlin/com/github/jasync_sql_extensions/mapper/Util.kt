package com.github.jasync_sql_extensions.mapper

import com.github.jasync.sql.db.RowData

class TestingRowData(
    val columnNames: List<String>,
    val data: List<Any?>,
    val rowNumber: Int
) : RowData, List<Any?> by data {
    override fun get(index: Int): Any? = data[index]
    override fun get(column: String): Any? = data[columnNames.indexOf(column)]
    override fun rowNumber(): Int = rowNumber
}