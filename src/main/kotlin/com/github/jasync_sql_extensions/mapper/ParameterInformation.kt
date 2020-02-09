package com.github.jasync_sql_extensions.mapper

/**
 * @author Leon Camus
 * @since 09.02.2020
 */
data class ParameterInformation(
        val name: String,
        val snakeCasedName: String,
        val isOptional: Boolean,
        val isNullable: Boolean,
        val isMappable: Boolean
)
