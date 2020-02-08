package com.github.jasync_sql_extensions

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ResultSet
import extension.PostgresExtension
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.stream.LongStream
import kotlin.streams.toList

/**
 * @author Leon Camus
 * @since 06.02.2020
 */
@ExtendWith(PostgresExtension::class)
class TestMapToBench {
    data class User(val id: Long?, val name: String, val shortName: String?)

    var users: List<User> = listOf()

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

        users = LongStream.range(1, 24000).mapToObj {
            User(it, RandomStringUtils.randomAlphabetic(64), RandomStringUtils.randomAlphabetic(64))
        }.toList()

        val timeMillis = System.currentTimeMillis()
        users.forEach { user ->
            connection.sendPreparedStatement(
                    "INSERT INTO \"user\" (name, short_name) VALUES (:user.name, :user.shortName);",
                    mapOf("user" to user)
            ).get()
        }
        println("Insert of users took: " + (System.currentTimeMillis() - timeMillis))
    }

    @Test
    fun testMap(connection: Connection) {
        val timeMillisSelect = System.currentTimeMillis()
        val resultSet = connection.sendPreparedStatement("""
            SELECT * FROM "user" ORDER BY id
        """).get().rows
        println("Select took: " + (System.currentTimeMillis() - timeMillisSelect))


        val timeMillis = System.currentTimeMillis()
        val mapped = doMap(resultSet)
        println("Mapping of the select took: " + (System.currentTimeMillis() - timeMillis))

        users.zip(mapped).forEach {
            Assertions.assertEquals(it.first, it.second)
        }

        val timeMillisReference = System.currentTimeMillis()
        val mappedRef = resultSet.map {
            User(it.getLong(0), it.getString(1)!!, it.getString(2))
        }
        println("Manual mapping of the select took: " + (System.currentTimeMillis() - timeMillisReference))

        users.zip(mappedRef).forEach {
            Assertions.assertEquals(it.first, it.second)
        }
    }

    fun doMap(resultSet: ResultSet): List<User> = resultSet.mapTo()
}