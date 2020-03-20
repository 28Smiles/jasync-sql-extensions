package com.github.jasync_sql_extensions.mapper.reflection

import com.github.jasync_sql_extensions.mapper.Mapper
import com.github.jasync_sql_extensions.mapper.MapperCreator
import com.github.jasync_sql_extensions.mapper.MapperCreator.CreatorIdentifier
import com.github.jasync_sql_extensions.mapper.asm.AsmMapperCreator
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

object ReflectionMapperCreator: MapperCreator {
    private val cache: LoadingCache<CreatorIdentifier<out Any>, Mapper<out Any>> =
            CacheBuilder.newBuilder()
                    .maximumSize(4096)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build(object : CacheLoader<CreatorIdentifier<out Any>, Mapper<out Any>>() {
                        override fun load(key: CreatorIdentifier<out Any>): Mapper<out Any> {
                            return create(key)
                        }
                    })

    @Suppress("UNCHECKED_CAST")
    override fun <Bean : Any> get(creatorIdentifier: CreatorIdentifier<Bean>): Mapper<Bean>
        = cache[creatorIdentifier] as Mapper<Bean>

    private fun create(creatorIdentifier: CreatorIdentifier<out Any>): Mapper<out Any>
        = ReflectionMapper(creatorIdentifier.clazz, creatorIdentifier.specials)
}