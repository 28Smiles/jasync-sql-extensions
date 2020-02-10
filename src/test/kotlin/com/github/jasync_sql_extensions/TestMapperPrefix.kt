package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync_sql_extensions.data.User
import com.github.jasync_sql_extensions.mapper.asm.cache
import extension.PostgresExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * @author Leon Camus
 * @since 09.02.2020
 */
@ExtendWith(PostgresExtension::class)
class TestMapperPrefix {
    @BeforeEach
    fun prepare(connection: Connection) {
        connection.sendQuery("""
            CREATE TABLE "user" (
                id BIGSERIAL NOT NULL,
                name VARCHAR(128) NOT NULL,
                short_name VARCHAR(128),
                alt_name VARCHAR(128),
                
                PRIMARY KEY (id)
            )
        """).get()
        connection.sendPreparedStatement("""
            INSERT INTO "user" (name, short_name) VALUES (?, ?)
        """, listOf("alfred", "alf")).get()
        connection.sendPreparedStatement("""
            INSERT INTO "user" (name, short_name) VALUES (?, ?)
        """, listOf("ralf", null)).get()
        connection.sendPreparedStatement("""
            INSERT INTO "user" (name, short_name) VALUES (?, ?)
        """, listOf("bertold", "bert")).get()
    }

    @Test
    fun testPrefix(connection: Connection) {
        val resultSet = connection.sendPreparedStatement("""
            SELECT id as "user_id", name as "user_name", short_name as "user_short_name" FROM "user" ORDER BY id
        """).get().rows

        val mapper = cache[User::class]
        val users = mapper.map(resultSet, "user_")

        Assertions.assertEquals(User(1, "alfred", "alf"), users[0])
        Assertions.assertEquals(User(2, "ralf", null), users[1])
        Assertions.assertEquals(User(3, "bertold", "bert"), users[2])
    }
}