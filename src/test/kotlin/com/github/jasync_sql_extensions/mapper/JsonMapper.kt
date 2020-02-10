package com.github.jasync_sql_extensions.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.jasync.sql.db.RowData
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
object JsonMapper: ComplexMapper {
    val mapper = ObjectMapper().registerKotlinModule()

    override fun canMap(type: KType): Boolean = type.isSubtypeOf(Json::class.starProjectedType)

    override fun map(type: KType, rowData: RowData, index: Int): Any? {
        return rowData.getString(index)?.let { mapper.readValue(it, type.jvmErasure.java) }
    }

    interface Json
}