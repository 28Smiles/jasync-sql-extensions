package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.length
import com.github.jasync_sql_extensions.data.Lang
import com.github.jasync_sql_extensions.data.UserJson
import com.google.common.util.concurrent.UncheckedExecutionException
import extension.PostgresExtension
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(PostgresExtension::class)
class TestPreparedStatementBinding {
    @Test
    fun oneBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :number
        """.trimIndent(), mapOf(
                "number" to 42
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(1, queryResult.rows[0].length)
        Assertions.assertEquals(42, queryResult.rows[0][0])
    }

    @Test
    fun multiBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :number, (:nabla)::timestamp, :foo
        """.trimIndent(), mapOf(
                "number" to 42,
                "foo" to "bar",
                "nabla" to Instant.ofEpochMilli(100000000L).toString()
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals(42, queryResult.rows[0][0])
        Assertions.assertEquals("bar", queryResult.rows[0][2])
        Assertions.assertEquals(
                100000000L,
                queryResult.rows[0].getDate(1)?.toDateTime(DateTimeZone.UTC)?.millis
        )
    }

    @Test
    fun beanBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :b.number, (:b.nabla)::timestamp, :b.foo
        """.trimIndent(), mapOf(
                "b" to TestObject1(42, "bar", Instant.ofEpochMilli(100000000L))
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals(42, queryResult.rows[0][0])
        Assertions.assertEquals("bar", queryResult.rows[0][2])
        Assertions.assertEquals(
                100000000L,
                queryResult.rows[0].getDate(1)?.toDateTime(DateTimeZone.UTC)?.millis
        )
    }

    @Test
    fun beanBindingUncached(connection: Connection) {
        val queryResult: QueryResult = connection.sendUncachedPreparedStatement("""
            SELECT :b.number, (:b.nabla)::timestamp, :b.foo
        """.trimIndent(), mapOf(
                "b" to TestObject1(42, "bar", Instant.ofEpochMilli(100000000L))
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals(42, queryResult.rows[0][0])
        Assertions.assertEquals("bar", queryResult.rows[0][2])
        Assertions.assertEquals(
                100000000L,
                queryResult.rows[0].getDate(1)?.toDateTime(DateTimeZone.UTC)?.millis
        )
    }

    data class TestObject1(val number: Long, val foo: String, val nabla: Instant)

    @Test
    fun beanOptionalBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :b?.a?.number, (:b?.a?.nabla)::timestamp, :b?.a?.foo
        """.trimIndent(), mapOf(
                "b" to TestObject2(TestObject1(42, "bar", Instant.ofEpochMilli(100000000L)))
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals(42, queryResult.rows[0][0])
        Assertions.assertEquals("bar", queryResult.rows[0][2])
        Assertions.assertEquals(
                100000000L,
                queryResult.rows[0].getDate(1)?.toDateTime(DateTimeZone.UTC)?.millis
        )
    }

    @Test
    fun beanJsonBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :b.name
        """, mapOf(
                "b" to UserJson(name = Lang("hello", "world"))
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(1, queryResult.rows[0].length)
        Assertions.assertEquals( "{\"de\":\"hello\",\"en\":\"world\"}", queryResult.rows[0][0])
    }

    @Test
    fun listBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT 42 in :list0, 156 in :list1, 156
        """, mapOf(
                "list0" to listOf(13, 156, 156, 1),
                "list1" to listOf(45, 156, 156, 42)
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals( false, queryResult.rows[0][0])
        Assertions.assertEquals( true, queryResult.rows[0][1])
        Assertions.assertEquals( 156, queryResult.rows[0][2])
    }

    @Test
    fun beanOptionalNullBinding(connection: Connection) {
        val queryResult: QueryResult = connection.sendPreparedStatement("""
            SELECT :b?.a?.number, (:b?.a?.nabla)::timestamp, :b?.a?.foo
        """.trimIndent(), mapOf(
                "b" to TestObject2(null)
        )).get()

        Assertions.assertEquals(1, queryResult.rows.length)
        Assertions.assertEquals(3, queryResult.rows[0].length)
        Assertions.assertEquals(null, queryResult.rows[0][0])
        Assertions.assertEquals(null, queryResult.rows[0][2])
        Assertions.assertEquals(null, queryResult.rows[0].getDate(1))
    }

    @Test
    fun beanBindingNull(connection: Connection) {
        Assertions.assertThrows(KotlinNullPointerException::class.java) {
            connection.sendPreparedStatement("""
                SELECT :b.a.number, (:b.a.nabla)::timestamp, :b.a.foo
            """.trimIndent(), mapOf(
                    "b" to TestObject2(null)
            ))
        }
    }

    @Test
    fun positionalBinding(connection: Connection) {
        try {
            connection.sendPreparedStatement("""
                SELECT ?, (:b.a.nabla)::timestamp, :b.a.foo
            """.trimIndent(), mapOf(
                    "b" to TestObject2(null)
            ))
        } catch (e: Exception) {
            Assertions.assertEquals(
                    IllegalArgumentException::class,
                    e.cause!!::class
            )
            return
        }
        Assertions.fail<String>("No exception encountered")
    }

    data class TestObject2(val a: TestObject1?)
}