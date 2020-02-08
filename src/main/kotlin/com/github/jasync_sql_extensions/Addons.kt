package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.util.length
import com.github.jasync_sql_extensions.compiler.createCompiledSupplierOrFallback
import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Function
import kotlin.reflect.KType

private val preparedCache: LoadingCache<String, Pair<String, (Map<String, Any?>) -> List<Any?>>> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(SqlPreprocessor)

fun Connection.sendPreparedStatement(
        query: String,
        args: Map<String, Any?>
): CompletableFuture<QueryResult> {
    val (positionalQuery, converter) = preparedCache[query]

    return this.sendPreparedStatement(positionalQuery, converter(args))
}

fun Connection.sendUncachedPreparedStatement(
        query: String,
        args: Map<String, Any?>
): CompletableFuture<QueryResult> {
    val (positionalQuery, converter) = SqlPreprocessor.load(query)

    return this.sendPreparedStatement(positionalQuery, converter(args))
}

fun String.toSnakeCased(): String {
    val stringBuilder = StringBuilder()
    this.forEach { c ->
        if (c.isUpperCase()) {
            stringBuilder.append('_').append(c.toLowerCase())
        } else {
            stringBuilder.append(c)
        }
    }

    return stringBuilder.toString()
}

val mappers: Map<KType, (RowData, Int) -> Any?> = mapOf()

class ConstructorInformation<Bean>(
        val constructor: Function<Array<Any?>, Bean>,
        val parameterType: Array<KType>,
        val parameterNullable: Array<Boolean>,
        val parameterColumnIndex: Array<Int?>,
        val args: Array<Any?>
)

inline fun <reified Bean : Any> ResultSet.mapTo(): List<Bean> {
    val row = this.columnNames().mapIndexed { index, s -> s to index }.toMap()
    val constructors = Bean::class.constructors.filter { function ->
        function.parameters.all { parameter ->
            row[parameter.name!!.toSnakeCased()] != null || parameter.isOptional || parameter.type.isMarkedNullable
        }
    }.map { function ->
        val parameters = function.parameters
        val length = parameters.length
        val types = Array(length) { parameters[it].type }

        ConstructorInformation<Bean>(
                createCompiledSupplierOrFallback(function),
                types,
                Array(length) { types[it].isMarkedNullable },
                Array(length) { row[parameters[it].name!!.toSnakeCased()] },
                Array(length) { null }
        )
    }.sortedByDescending { it.parameterColumnIndex.size }

    return this.map { rowData ->
        val constructor = constructors.map {
                    for (i in it.args.indices) {
                        val mapper = mappers[it.parameterType[i]]
                        it.args[i] = it.parameterColumnIndex[i]?.let {  index ->
                            mapper?.let { it(rowData, index) } ?: rowData[index]
                        }
                    }
                    it
                }.first {
                    var b = true
                    for (i in it.args.indices) {
                        if (it.args[i] == null && !it.parameterNullable[i]) {
                            b = false
                            break
                        }
                    }
                    b
                }

        constructor.constructor.apply(constructor.args)
    }
}
