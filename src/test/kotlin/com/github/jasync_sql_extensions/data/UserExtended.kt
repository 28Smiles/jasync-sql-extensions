package com.github.jasync_sql_extensions.data

/**
 * @author Leon Camus
 * @since 09.02.2020
 */
data class UserExtended(
        val id: Long?,
        val name: String,
        val shortName: String?,
        val parent: List<User> = listOf()
)
