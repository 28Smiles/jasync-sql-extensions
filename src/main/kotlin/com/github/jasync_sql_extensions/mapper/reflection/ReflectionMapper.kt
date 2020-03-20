package com.github.jasync_sql_extensions.mapper.reflection

import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.mapper.Mapper
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal class ReflectionMapper<Bean : Any>(
    clazz: KClass<Bean>,
    specials: Set<String> = setOf()
) : Mapper<Bean>(clazz, specials) {
    val primaryConstructor = clazz.primaryConstructor
            ?: throw NullPointerException("No primary constructor found, is $clazz not a Kotlin Class?")
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
                            ?: if (parameterInformation[index].isNullable || parameterInformation[index].isOptional) null
                            else throw NullPointerException()
                } else {
                    if (index == baked.size) {
                        optionals
                    } else {
                        null
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            return constructor.newInstance(*args) as Bean
        } else {
            val args = Array(baked.size) { index ->
                baked[index](rowData)
                        ?: if (parameterInformation[index].isNullable || parameterInformation[index].isOptional) null
                        else throw NullPointerException()
            }
            return primaryConstructor.call(*args)
        }
    }
}