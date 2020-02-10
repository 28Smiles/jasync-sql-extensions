package com.github.jasync_sql_extensions.data

import java.time.Instant

/**
 * @author Leon Camus
 * @since 11.02.2020
 */
data class UserInstant(
        val id: Long?,
        val created: Instant,
        val shortName: String?
)
