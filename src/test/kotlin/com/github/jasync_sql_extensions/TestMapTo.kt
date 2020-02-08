package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.map
import extension.PostgresExtension
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

/**
 * @author Leon Camus
 * @since 06.02.2020
 */
@ExtendWith(PostgresExtension::class)
class TestMapTo {
    data class User(val id: Long?, val name: String, val shortName: String?)
    data class UserExtended(val id: Long?, val name: String, val shortName: String?, val addid: LongRange?)

    @BeforeEach
    fun prepare(connection: Connection) {
        connection.sendQuery("""
            CREATE TABLE "user" (
                id BIGSERIAL NOT NULL,
                name VARCHAR(128) NOT NULL,
                short_name VARCHAR(128),
                
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
    fun testMap(connection: Connection) {
        val users = connection.sendPreparedStatement("""
            SELECT * FROM "user" ORDER BY id
        """).get().rows.mapTo<User>()

        Assertions.assertEquals(User(1, "alfred", "alf"), users[0])
        Assertions.assertEquals(User(2, "ralf", null), users[1])
        Assertions.assertEquals(User(3, "bertold", "bert"), users[2])
    }

    @Test
    fun testMapNotInSelection(connection: Connection) {
        val users = connection.sendPreparedStatement("""
            SELECT * FROM "user" ORDER BY id
        """).get().rows.mapTo<UserExtended>()

        Assertions.assertEquals(UserExtended(1, "alfred", "alf", null), users[0])
        Assertions.assertEquals(UserExtended(2, "ralf", null, null), users[1])
        Assertions.assertEquals(UserExtended(3, "bertold", "bert", null), users[2])
    }
}