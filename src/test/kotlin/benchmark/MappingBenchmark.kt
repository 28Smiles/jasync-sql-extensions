package benchmark

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ResultSet
import com.github.jasync_sql_extensions.mapTo
import com.github.jasync_sql_extensions.mapper.ReflectionMapper
import com.github.jasync_sql_extensions.sendPreparedStatement
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
class MappingBenchmark {
    data class User(val id: Long, val name: String, val shortName: String?)

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

        val amount = 500000L
        users = LongStream.range(1, amount).mapToObj {
            User(it, RandomStringUtils.randomAlphabetic(64), RandomStringUtils.randomAlphabetic(64))
        }.toList()

        val timeMillis = System.currentTimeMillis()
        users.map { user ->
            connection.sendPreparedStatement(
                    "INSERT INTO \"user\" (id, name, short_name) VALUES (:user.id, :user.name, :user.shortName);",
                    mapOf("user" to user)
            )
        }.forEach { it.get() }
        println("Insert of $amount users took: ${System.currentTimeMillis() - timeMillis}")
    }

    @Test
    fun testMap(connection: Connection) {
        val resultSet = connection.sendPreparedStatement("""
            SELECT * FROM "user" ORDER BY id
        """).get().rows

        users.zip(mapManual(resultSet)).forEach {
            Assertions.assertEquals(it.first, it.second)
        }

        users.zip(mapReflection(resultSet)).forEach {
            Assertions.assertEquals(it.first, it.second)
        }

        users.zip(mapASMCold(resultSet)).forEach {
            Assertions.assertEquals(it.first, it.second)
        }

        users.zip(mapASMHot(resultSet)).forEach {
            Assertions.assertEquals(it.first, it.second)
        }
    }

    fun mapManual(resultSet: ResultSet): List<User> {
        val timeMillis = System.currentTimeMillis()
        val users = resultSet.map {
            User(it.getLong(0)!!, it.getString(1)!!, it.getString(2))
        }
        println("Manual mapping of the select took: ${System.currentTimeMillis() - timeMillis}")

        return users
    }

    fun mapReflection(resultSet: ResultSet): List<User> {
        val timeMillis = System.currentTimeMillis()
        val mapper = ReflectionMapper(User::class)
        val users = mapper.map(resultSet)
        println("Reflection mapping of the select took: ${System.currentTimeMillis() - timeMillis}")

        return users
    }

    fun mapASMCold(resultSet: ResultSet): List<User> {
        val timeMillis = System.currentTimeMillis()
        val users = resultSet.mapTo<User>()
        println("Cold ASM mapping of the select took: ${System.currentTimeMillis() - timeMillis}")

        return users
    }

    fun mapASMHot(resultSet: ResultSet): List<User> {
        val timeMillis = System.currentTimeMillis()
        val users = resultSet.mapTo<User>()
        println("Hot ASM mapping of the select took: ${System.currentTimeMillis() - timeMillis}")

        return users
    }
}