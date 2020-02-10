package com.github.jasync_sql_extensions.mapper.reflection

import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.MapperCreator
import com.github.jasync_sql_extensions.mapper.asm.AsmMapperCreator
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
object ReflectionMapperCreator: MapperCreator {
    private val cache: LoadingCache<KClass<out Any>, Mapper<out Any>> =
            CacheBuilder.newBuilder()
                    .maximumSize(4096)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(object : CacheLoader<KClass<out Any>, Mapper<out Any>>() {
                        override fun load(key: KClass<out Any>): Mapper<out Any> {
                            return create(key)
                        }
                    })

    @Suppress("UNCHECKED_CAST")
    override fun <Bean: Any>get(clazz: KClass<Bean>): Mapper<Bean> = cache[clazz] as Mapper<Bean>

    private fun create(clazz: KClass<out Any>): Mapper<out Any> = ReflectionMapper(clazz)
}