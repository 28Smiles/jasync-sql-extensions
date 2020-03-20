package com.github.jasync_sql_extensions.data

data class UserExtended(
        val id: Long?,
        val name: String,
        val shortName: String?,
        val parent: List<User> = listOf()
)
