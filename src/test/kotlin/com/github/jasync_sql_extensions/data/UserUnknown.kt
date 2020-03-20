package com.github.jasync_sql_extensions.data

data class UserUnknown(
        val id: Long?,
        val name: String,
        val shortName: Map<String, String> = mapOf()
)
