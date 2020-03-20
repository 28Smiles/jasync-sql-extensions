package com.github.jasync_sql_extensions.mapper.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.Json
import com.github.jasync_sql_extensions.mapper.ComplexMapper
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

object JsonMapper: ComplexMapper {
    val mapper = ObjectMapper().registerKotlinModule()

    override fun canMap(type: KType): Boolean = type.isSubtypeOf(Json::class.starProjectedType)

    override fun map(type: KType, rowData: RowData, index: Int): Any? {
        return rowData.getString(index)?.let { mapper.readValue(it, type.jvmErasure.java) }
    }
}