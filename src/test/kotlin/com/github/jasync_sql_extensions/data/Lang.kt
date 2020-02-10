package com.github.jasync_sql_extensions.data

import com.github.jasync_sql_extensions.mapper.JsonMapper

/**
 * @author Leon Camus
 * @since 11.02.2020
 */
data class Lang(
        val de: String,
        val en: String
): JsonMapper.Json
