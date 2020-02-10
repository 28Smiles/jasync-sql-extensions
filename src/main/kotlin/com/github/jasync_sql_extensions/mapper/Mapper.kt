package com.github.jasync_sql_extensions.mapper

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.util.length
import com.github.jasync_sql_extensions.toSnakeCased
import org.joda.time.DateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

/**
 * @author Leon Camus
 * @since 08.02.2020
 */
abstract class Mapper<Bean : Any>(clazz: KClass<Bean>) {
    private val parameters = clazz.primaryConstructor!!.parameters
    val parameterInformation = parameters.map {
        ParameterInformation(
                it.name!!,
                it.name!!.toSnakeCased(),
                it.isOptional,
                it.type.isMarkedNullable,
                primitiveMappers.contains(it.type)
        )
    }.toTypedArray()

    val mappers: Array<((RowData, Int) -> Any?)?> = Array(parameters.length) { i ->
        val parameter = parameters[i]
        val type = parameter.type
        findMapper(type) ?: if (type.isMarkedNullable || parameter.isOptional) null
                else throw NullPointerException("No mapper found for ${parameters[i]}, " +
                        "but is not marked nullable, nor optional.")
    }

    fun map(resultSet: ResultSet, prefix: String = ""): List<Bean> {
        val columnNames = resultSet.columnNames().mapIndexed { i, s -> s to i }.toMap()
        val columnIds: Array<Int?> = Array(parameterInformation.size) { i ->
            columnNames[prefix + parameterInformation[i].snakeCasedName]
                    ?: if (parameterInformation[i].isNullable || parameterInformation[i].isOptional) null
                    else throw NullPointerException(
                            "No column found for ${parameterInformation[i].name}" +
                                    " and parameter is not marked as optional nor nullable.")
        }

        return doMap(resultSet, columnIds)
    }

    private fun doMap(resultSet: ResultSet, columnIds: Array<Int?>): List<Bean> {
        // Calculate enabled optionals
        var optionals = 0
        columnIds.zip(mappers).forEachIndexed { index, param ->
            if (param.first == null || param.second == null) {
                val pow = 1 shl index

                optionals = optionals or pow
            }
        }

        val baked = Array(columnIds.size) { index ->
            val col = columnIds[index]
            val mapper = mappers[index]
            { rowData: RowData ->
                col?.let { column -> mapper?.let { it(rowData, column) } }
            }
        }

        return resultSet.map { construct(it, optionals, baked) }
    }

    abstract fun construct(rowData: RowData, optionals: Int, baked: Array<(RowData) -> Any?>): Bean

    companion object {
        private val primitiveMappers: Map<KType, (RowData, Int) -> Any?> = mapOf(
                Long::class.starProjectedType to { rowData, index -> rowData.getLong(index) },
                Long::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getLong(index) },
                Int::class.starProjectedType to { rowData, index -> rowData.getInt(index) },
                Int::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getInt(index) },
                Byte::class.starProjectedType to { rowData, index -> rowData.getByte(index) },
                Byte::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getByte(index) },
                String::class.starProjectedType to { rowData, index -> rowData.getString(index) },
                String::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getString(index) },
                Boolean::class.starProjectedType to { rowData, index -> rowData.getBoolean(index) },
                Boolean::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getBoolean(index) },
                Float::class.starProjectedType to { rowData, index -> rowData.getFloat(index) },
                Float::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getFloat(index) },
                Double::class.starProjectedType to { rowData, index -> rowData.getDouble(index) },
                Double::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getDouble(index) },
                DateTime::class.starProjectedType to { rowData, index -> rowData.getDate(index) },
                DateTime::class.starProjectedType.withNullability(true) to { rowData, index -> rowData.getDate(index) }
        )

        private val customMappers: MutableMap<KType, (RowData, Int) -> Any?> = mutableMapOf()
        private val customComplexMappers: MutableList<ComplexMapper> = mutableListOf()

        fun findMapper(type: KType): ((RowData, Int) -> Any?)? = primitiveMappers[type]
                ?: customMappers[type]
                ?: customComplexMappers.find { it.canMap(type) }?.let { { rowData: RowData, index: Int ->
                    it.map(type, rowData, index)
                } }

        fun register(type: KType, mapper: (RowData, Int) -> Any?) {
            customMappers[type] = mapper
        }

        fun register(mapper: ComplexMapper) {
            customComplexMappers.add(mapper)
        }
    }
}
