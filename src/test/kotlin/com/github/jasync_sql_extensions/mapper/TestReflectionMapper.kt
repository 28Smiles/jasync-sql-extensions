package com.github.jasync_sql_extensions.mapper

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.data.JavaUser
import com.github.jasync_sql_extensions.data.Lang
import com.github.jasync_sql_extensions.data.Numbers
import com.github.jasync_sql_extensions.data.Unmappable
import com.github.jasync_sql_extensions.data.User
import com.github.jasync_sql_extensions.data.UserExtended
import com.github.jasync_sql_extensions.data.UserInstant
import com.github.jasync_sql_extensions.data.UserJson
import com.github.jasync_sql_extensions.data.UserUnknown
import com.github.jasync_sql_extensions.mapTo
import com.github.jasync_sql_extensions.mapper.asm.AsmMapperCreator
import com.github.jasync_sql_extensions.mapper.asm.MapperSynthesizer
import com.github.jasync_sql_extensions.mapper.reflection.ReflectionMapper
import com.github.jasync_sql_extensions.mapper.reflection.ReflectionMapperCreator
import com.google.common.util.concurrent.UncheckedExecutionException
import extension.PostgresExtension
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import kotlin.reflect.full.starProjectedType

@ExtendWith(PostgresExtension::class)
class TestReflectionMapper {
    val mapperCreator: MapperCreator = ReflectionMapperCreator

    @Nested
    inner class Simple {
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
        fun testMap(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            val users = resultSet.mapTo<User>(
                    mapperCreator = mapperCreator
            )

            Assertions.assertEquals(User(1, "alfred", "alf"), users[0])
            Assertions.assertEquals(User(2, "ralf", null), users[1])
            Assertions.assertEquals(User(3, "bertold", "bert"), users[2])
        }

        @Test
        fun testMapNPE(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT id, short_name, alt_name AS "name" FROM "user" ORDER BY id
            """).get().rows

            Assertions.assertThrows(NullPointerException::class.java) {
                resultSet.mapTo<User>(
                        mapperCreator = mapperCreator
                )
            }
        }

        @Test
        fun testMapNPEOptional(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT id, short_name, alt_name AS "name" FROM "user" ORDER BY id
            """).get().rows

            Assertions.assertThrows(NullPointerException::class.java) {
                resultSet.mapTo<UserExtended>(
                        mapperCreator = mapperCreator
                )
            }
        }

        @Test
        fun testUnmappableBean() {
            Assertions.assertThrows(NullPointerException::class.java) {
                ReflectionMapper(Unmappable::class)
            }
        }

        @Test
        fun testUnmappableColumn(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT id FROM "user" ORDER BY id
            """).get().rows

            Assertions.assertThrows(NullPointerException::class.java) {
                resultSet.mapTo<User>(
                        mapperCreator = mapperCreator
                )
            }
        }

        @Test
        fun testMapOptional(connection: Connection) {
            // Optionals name is not in selection
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            val users = resultSet.mapTo<UserExtended>(
                    mapperCreator = mapperCreator
            )

            Assertions.assertEquals(UserExtended(1, "alfred", "alf"), users[0])
            Assertions.assertEquals(UserExtended(2, "ralf", null), users[1])
            Assertions.assertEquals(UserExtended(3, "bertold", "bert"), users[2])
        }

        @Test
        fun testMapUnknownOptional(connection: Connection) {
            // Optionals name is in selection, but not mappable
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            val users = resultSet.mapTo<UserUnknown>(
                    mapperCreator = mapperCreator
            )

            Assertions.assertEquals(UserUnknown(1, "alfred"), users[0])
            Assertions.assertEquals(UserUnknown(2, "ralf"), users[1])
            Assertions.assertEquals(UserUnknown(3, "bertold"), users[2])
        }

        @Test
        fun testPrefix(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT id as "user_id", name as "user_name", short_name as "user_short_name" FROM "user" ORDER BY id
            """).get().rows


            val users = resultSet.mapTo<User>(
                    prefix = "user_",
                    mapperCreator = ReflectionMapperCreator
            )

            Assertions.assertEquals(User(1, "alfred", "alf"), users[0])
            Assertions.assertEquals(User(2, "ralf", null), users[1])
            Assertions.assertEquals(User(3, "bertold", "bert"), users[2])
        }

        @Test
        fun testJavaUser(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            Assertions.assertThrows(UncheckedExecutionException::class.java) {
                resultSet.mapTo<JavaUser>(
                        mapperCreator = AsmMapperCreator
                )
            }
        }
    }

    @Nested
    inner class Primitives {
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

            Assertions.assertEquals(1, numbers.size)
            Assertions.assertEquals(
                    Numbers(156415616165L,
                            45456,
                            1231.15661F,
                            165561.4544446,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false
                    ),
                    numbers[0]
            )
        }
    }

    @Nested
    inner class CustomMapper {
        @BeforeEach
        fun prepare(connection: Connection) {
            connection.sendQuery("""
                CREATE TABLE "user" (
                    id BIGSERIAL NOT NULL,
                    created TIMESTAMP NOT NULL,
                    short_name VARCHAR(128),
                    
                    PRIMARY KEY (id)
                )
            """).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (created, short_name) VALUES (?, ?)
            """, listOf(Instant.ofEpochMilli(1561566156L), "alf")).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (created, short_name) VALUES (?, ?)
            """, listOf(Instant.ofEpochMilli(15615156L), null)).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (created, short_name) VALUES (?, ?)
            """, listOf(Instant.ofEpochMilli(156156645156L), "bert")).get()
        }

        @Test
        fun testMap(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            Mapper.register(Instant::class.starProjectedType) { rowData: RowData, index: Int ->
                Instant.ofEpochMilli(rowData.getDate(index)!!.toDateTime(DateTimeZone.UTC).millis)
            }
            val users = resultSet.mapTo<UserInstant>(
                    mapperCreator = mapperCreator
            )

            Assertions.assertEquals(
                    UserInstant(1, Instant.ofEpochMilli(1561566156L), "alf"),
                    users[0])
            Assertions.assertEquals(
                    UserInstant(2, Instant.ofEpochMilli(15615156L), null),
                    users[1])
            Assertions.assertEquals(
                    UserInstant(3, Instant.ofEpochMilli(156156645156L), "bert"),
                    users[2])
        }
    }

    @Nested
    inner class Json {
        @BeforeEach
        fun prepare(connection: Connection) {
            connection.sendQuery("""
                CREATE TABLE "user" (
                    id BIGSERIAL NOT NULL,
                    name JSON NOT NULL,
                    short_name VARCHAR(128),
                    
                    PRIMARY KEY (id)
                )
            """).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (name, short_name) VALUES (?, ?)
            """, listOf("{ \"de\": \"Alfred\", \"en\": \"Alf\" }", "alf")).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (name, short_name) VALUES (?, ?)
            """, listOf("{ \"de\": \"Ralf\", \"en\": \"rulf\" }", null)).get()
            connection.sendPreparedStatement("""
                INSERT INTO "user" (name, short_name) VALUES (?, ?)
            """, listOf("{ \"de\": \"Berthold\", \"en\": \"Bert\" }", "bert")).get()
        }

        @Test
        fun testMap(connection: Connection) {
            val resultSet = connection.sendPreparedStatement("""
                SELECT * FROM "user" ORDER BY id
            """).get().rows

            val users = resultSet.mapTo<UserJson>(
                    mapperCreator = mapperCreator
            )

            Assertions.assertEquals(UserJson(1, Lang("Alfred", "Alf"), "alf"), users[0])
            Assertions.assertEquals(UserJson(2, Lang("Ralf", "rulf"), null), users[1])
            Assertions.assertEquals(UserJson(3, Lang("Berthold", "Bert"), "bert"), users[2])
        }
    }
}
