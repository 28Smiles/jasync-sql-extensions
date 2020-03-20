package com.github.jasync_sql_extensions.mapper

import kotlin.reflect.KClass

interface MapperCreator {
    operator fun <Bean : Any>get(creatorIdentifier: CreatorIdentifier<Bean>): Mapper<Bean>

    data class CreatorIdentifier<B : Any>(val clazz: KClass<B>, val specials: Set<String> = setOf())
}
