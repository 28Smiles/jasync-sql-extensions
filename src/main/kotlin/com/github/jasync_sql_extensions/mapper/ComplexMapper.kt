package com.github.jasync_sql_extensions.mapper

import com.github.jasync.sql.db.RowData
import kotlin.reflect.KType

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
interface ComplexMapper {
    fun canMap(type: KType): Boolean
    fun map(type: KType, rowData: RowData, index: Int): Any?
}