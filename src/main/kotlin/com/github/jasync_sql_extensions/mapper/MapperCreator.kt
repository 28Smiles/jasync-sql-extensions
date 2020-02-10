package com.github.jasync_sql_extensions.mapper

import kotlin.reflect.KClass

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
interface MapperCreator {
    operator fun <Bean : Any>get(clazz: KClass<Bean>): Mapper<Bean>
}
