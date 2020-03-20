package com.github.jasync_sql_extensions.data

import java.time.Instant

data class UserInstant(
        val id: Long?,
        val created: Instant,
        val shortName: String?
)
