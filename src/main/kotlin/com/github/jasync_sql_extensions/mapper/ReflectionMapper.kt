package com.github.jasync_sql_extensions.mapper

import com.github.jasync.sql.db.RowData
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * @author Leon Camus
 * @since 09.02.2020
 */
class ReflectionMapper<Bean : Any>(clazz: KClass<Bean>) : Mapper<Bean>(clazz) {
    val primaryConstructor = clazz.primaryConstructor!!
    val constructor = clazz.java.constructors.find { constructor ->
        constructor.parameters.any {
            it.type.name == "kotlin.jvm.internal.DefaultConstructorMarker"
        }
    }

    override fun construct(rowData: RowData, optionals: Int, baked: Array<(RowData) -> Any?>): Bean {
        if (constructor != null) {
            val args = Array(baked.size + 2) { index ->
                if (index < baked.size) {
                    baked[index](rowData)
                } else {
                    if (index == baked.size) {
                        optionals
                    } else {
                        null
                    }
                }
            }
            return constructor.newInstance(*args) as Bean
        } else {
            val args = Array(baked.size) { index ->
                baked[index](rowData)
            }
            return primaryConstructor.call(*args)
        }
    }
}