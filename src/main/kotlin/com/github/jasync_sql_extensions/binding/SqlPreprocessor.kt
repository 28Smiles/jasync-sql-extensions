package com.github.jasync_sql_extensions.binding

import com.github.jasync_sql_extensions.SqlLexer
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Token
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * @author Leon Camus
 * @since 07.02.2020
 */
object SqlPreprocessor : CacheLoader<String, Pair<Array<String>, (Map<String, Any?>) -> List<Any?>>>() {
    private val SPLITTER = "(\\.)|(\\?\\.)|([^[.|?.]]+)".toRegex()
    private val SAFE_CALL = "?."
    private val CALL = "."

    override fun load(key: String): Pair<Array<String>, (Map<String, Any?>) -> List<Any?>> {
        val tokenizer = SqlLexer(CharStreams.fromString(key))
        val sqlOutBuffer = StringBuilder()
        val sqlPartsOut = mutableListOf<String>()
        val routes = mutableListOf<String>()
        var token: Token? = tokenizer.nextToken()

        while (token != null && token.type != SqlLexer.EOF) {
            when (token.type) {
                SqlLexer.NAMED_PARAM -> {
                    sqlPartsOut.add(sqlOutBuffer.toString())
                    sqlOutBuffer.clear()
                    routes.add(token.text.substring(1))
                }
                SqlLexer.POSITIONAL_PARAM ->
                    throw IllegalArgumentException("Positional Arguments are Prohibited with the use of Bindings.")
                else -> sqlOutBuffer.append(token.text)
            }

            token = tokenizer.nextToken()
        }

        return sqlPartsOut.toTypedArray() to { bindings ->
            routes.map { route ->
                if (route.contains('.')) {
                    val parts = SPLITTER.findAll(route).map { it.value }.iterator()
                    val accessor = object {
                        operator fun invoke(path: Iterator<String>, bean: Any?): Any? {
                            if (!path.hasNext()) {
                                return bean
                            }

                            val operator = path.next()
                            val name = path.next()

                            if (bean == null && operator == SAFE_CALL) {
                                return this(path, bean)
                            }

                            return this(path, bean!!.get(name))
                        }
                    }

                    accessor(parts, bindings[parts.next()])
                } else {
                    bindings[route]
                }
            }
        }
    }
}

private val fieldCache: LoadingCache<Pair<KClass<*>, String>, KProperty1.Getter<out Any, Any?>> = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(object : CacheLoader<Pair<KClass<*>, String>, KProperty1.Getter<out Any, Any?>>() {
            override fun load(key: Pair<KClass<*>, String>): KProperty1.Getter<out Any, Any?> {
                return (key.first.memberProperties.find {
                    it.name == key.second
                } ?: throw IllegalArgumentException()).getter
            }
        })

private fun Any.get(field: String): Any? {
    return fieldCache.get(this::class to field).call(this)
}
