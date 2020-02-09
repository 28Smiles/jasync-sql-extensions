package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync_sql_extensions.data.User
import extension.PostgresExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * @author Leon Camus
 * @since 09.02.2020
 */
@ExtendWith(PostgresExtension::class)
class TestMapperPrimitives {
    data class Numbers(
            val i0: Long,
            val i1: Int,
            val i2: Float,
            val i3: Double,
            val i4: Boolean,
            val i5: Boolean,
            val i6: Boolean,
            val i7: Boolean,
            val i8: Boolean,
            val i9: Boolean
    )

    @BeforeEach
    fun prepare(connection: Connection) {
        connection.sendQuery("""
            CREATE TABLE "numbers" (
                i0 BIGSERIAL NOT NULL,
                i1 INTEGER NOT NULL,
                i2 REAL NOT NULL,
                i3 DOUBLE PRECISION NOT NULL,
                i4 BOOLEAN NOT NULL,
                i5 BOOLEAN NOT NULL,
                i6 BOOLEAN NOT NULL,
                i7 BOOLEAN NOT NULL,
                i8 BOOLEAN NOT NULL,
                i9 BOOLEAN NOT NULL,
                
                PRIMARY KEY (i0)
            )
        """).get()
        connection.sendPreparedStatement("""
            INSERT INTO "numbers" (i0, i1, i2, i3, i4, i5, i6, i7, i8, i9) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, listOf(
                156415616165L,
                45456,
                1231.15661F,
                165561.4544446,
                false,
                false,
                false,
                false,
                false,
                false
        )).get()
    }

    @Test
    fun testPrimitives(connection: Connection) {
        val numbers = connection.sendPreparedStatement("""
            SELECT * FROM "numbers"
        """).get().rows.mapTo<Numbers>()

        println(numbers[0])
    }
}