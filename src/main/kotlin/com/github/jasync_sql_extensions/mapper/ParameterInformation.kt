package com.github.jasync_sql_extensions.mapper

data class ParameterInformation(
        val name: String,
        val snakeCasedName: String,
        val isOptional: Boolean,
        val isNullable: Boolean,
        val isMappable: Boolean
)
