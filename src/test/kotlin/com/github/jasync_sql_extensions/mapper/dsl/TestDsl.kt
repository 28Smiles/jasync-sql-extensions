package com.github.jasync_sql_extensions.mapper.dsl

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import com.github.jasync_sql_extensions.mapper.TestingRowData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestDsl {
    @Test
    fun testFlat() {
        val columNames = listOf("mod0_id", "mod0_semester_id", "mod0_subject_id", "mod1_id", "mod2_id")
        val list = object : ResultSet, List<RowData> by listOf(
            TestingRowData(columNames, listOf(1L, 1L, 1L, 1L, 1L), 1),
            TestingRowData(columNames, listOf(2L, 3L, 1L, 1L, 3L), 2),
            TestingRowData(columNames, listOf(3L, 4L, 3L, 3L, 4L), 3)
        ) {
            override fun columnNames(): List<String> = columNames
        }.project<Course> {
            distinct(Course::id).prefix("mod0_").map {
                one(Course::semester) {
                    prefix("mod2_")
                    distinct(Semester::id)
                }
                one(Course::subject) {
                    prefix("mod1_")
                    distinct(Subject::id)
                }
            }
        }

        Assertions.assertEquals(3, list.size)
        Assertions.assertEquals(
            Course(id = 1, semesterId = 1, semester = Semester(id = 1), subjectId = 1, subject = Subject(id = 1)),
            list[0]
        )
        Assertions.assertEquals(
            Course(id = 2, semesterId = 3, semester = Semester(id = 1), subjectId = 1, subject = Subject(id = 1)),
            list[1]
        )
        Assertions.assertEquals(
            Course(id = 3, semesterId = 4, semester = Semester(id = 1), subjectId = 3, subject = Subject(id = 1)),
            list[2]
        )
    }

    data class Course(val id: Long, val semesterId: Long, val semester: Semester, val subjectId: Long, val subject: Subject)
    data class Semester(val id: Long)
    data class Subject(val id: Long)

    @Test
    fun `test 1 to n`() {
        val columNames = listOf("mod0_id", "mod1_right_id", "mod1_id")
        val list = object : ResultSet, List<RowData> by listOf(
            TestingRowData(columNames, listOf(1L, 1L, 1L), 1),
            TestingRowData(columNames, listOf(1L, 1L, 2L), 2),
            TestingRowData(columNames, listOf(1L, 1L, 3L), 3),
            TestingRowData(columNames, listOf(2L, 2L, 4L), 4),
            TestingRowData(columNames, listOf(2L, 2L, 1L), 5)
        ) {
            override fun columnNames(): List<String> = columNames
        }.project<Role> {
            distinct(Role::id).prefix("mod0_").map {
                group(Role::rights) {
                    prefix("mod1_")
                    distinct(Right::id)
                }
            }
        }

        Assertions.assertEquals(2, list.size)
        Assertions.assertEquals(
            Role(1, listOf(
                Right(1, 1),
                Right(1, 2),
                Right(1, 3)
            )),
            list[0]
        )
        Assertions.assertEquals(
            Role(2, listOf(
                Right(2, 4),
                Right(2, 1)
            )),
            list[1]
        )
    }

    data class Role(val id: Long, val rights: List<Right>)
    data class Right(val rightId: Long, val id: Long)
}
