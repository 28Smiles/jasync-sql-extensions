package com.github.jasync_sql_extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.binding.SqlPreprocessor
import com.github.jasync_sql_extensions.mapper.MapperCreator
import com.github.jasync_sql_extensions.mapper.MapperCreator.CreatorIdentifier
import com.github.jasync_sql_extensions.mapper.asm.AsmMapperCreator
import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

private val objectMapper = ObjectMapper().registerKotlinModule()

private val preparedCache: LoadingCache<String, Pair<Array<String>, (Map<String, Any?>) -> List<Any?>>> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(SqlPreprocessor)

fun Connection.sendPreparedStatement(
        query: String,
        args: Map<String, Any?>,
        jsonMapper: (Any) -> String = objectMapper::writeValueAsString,
        preprocessor: (String) -> Pair<Array<String>, (Map<String, Any?>) -> List<Any?>> = preparedCache::get
): CompletableFuture<QueryResult> {
    val (queryParts, converter) = preprocessor(query)

    val convertedArgs = converter(args).map {
        it?.let {
            if (it is Json) {
                jsonMapper(it)
            } else {
                it
            }
        }
    }

    assert(convertedArgs.size == queryParts.size - 1) {
        // Will Hopefully never be thrown
        throw IllegalArgumentException("More arguments where converted than there is space in the query!" +
                " This is an error in the library, please report.")
    }

    return this.sendPreparedStatement(
            convertedArgs.zip(queryParts).joinToString(separator = "") { (argument, queryPart) ->
                when (argument) {
                    is Collection<*> -> queryPart + "( ${argument.joinToString(separator = ",") { " ? " }} )"
                    else -> "$queryPart ? "
                }
            } + queryParts.last(),
            convertedArgs.flatMap {
                when (it) {
                    is Collection<*> -> it
                    else -> listOf(it)
                }
            })
}

fun Connection.sendUncachedPreparedStatement(
        query: String,
        args: Map<String, Any?>,
        jsonMapper: (Any) -> String = objectMapper::writeValueAsString
): CompletableFuture<QueryResult> {
    return this.sendPreparedStatement(
            query = query,
            args = args,
            jsonMapper = jsonMapper,
            preprocessor = SqlPreprocessor::load
    )
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

inline fun <reified Bean : Any> ResultSet.mapTo(
    prefix: String = "",
    mapperCreator: MapperCreator = AsmMapperCreator
): List<Bean> {
    @Suppress("UNCHECKED_CAST")
    return mapperCreator[CreatorIdentifier(Bean::class)].map(this, prefix).asSequence().toList()
}

inline fun <reified Bean : Any> Iterable<RowData>.mapTo(
    columnNames: List<String>,
    prefix: String = "",
    mapperCreator: MapperCreator = AsmMapperCreator
): List<Bean> {
    @Suppress("UNCHECKED_CAST")
    return mapperCreator[CreatorIdentifier(Bean::class)].map(this.iterator(), columnNames, prefix).asSequence().toList()
}

fun <Bean : Any> Iterator<RowData>.mapTo(
    columnNames: List<String>,
    beanClass: KClass<Bean>,
    prefix: String = "",
    specials: Set<String> = setOf(),
    mapperCreator: MapperCreator = AsmMapperCreator
): Iterator<Bean> {
    @Suppress("UNCHECKED_CAST")
    return mapperCreator[CreatorIdentifier(beanClass, specials)].map(this, columnNames, prefix)
}
