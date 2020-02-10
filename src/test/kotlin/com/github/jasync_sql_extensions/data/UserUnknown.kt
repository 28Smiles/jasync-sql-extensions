package com.github.jasync_sql_extensions.data

/**
 * @author Leon Camus
 * @since 10.02.2020
 */
data class UserUnknown(
        val id: Long?,
        val name: String,
        val shortName: Map<String, String> = mapOf()
)
