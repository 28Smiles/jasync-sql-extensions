package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.asm.cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
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

inline fun <reified Bean : Any> ResultSet.mapTo(prefix: String = ""): List<Bean> {
    @Suppress("UNCHECKED_CAST")
    return cache[Bean::class].map(this, prefix) as List<Bean>
}
