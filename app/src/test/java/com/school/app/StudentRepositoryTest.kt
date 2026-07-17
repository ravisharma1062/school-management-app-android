package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.repository.StudentRepository
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.Student
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class StudentRepositoryTest {

    private val api = mockk<ApiService>()
    private val repository = StudentRepository(api)

    private fun student(id: String, section: String, rollNo: String) = Student(
        id = id, name = "Student $id", rollNo = rollNo, studentClass = "5",
        section = section, dob = "2015-01-01",
    )

    @Test
    fun `allInClass pages through every result then filters and sorts`() = runTest {
        coEvery { api.students(0, 100, studentClass = "5") } returns PageResponse(
            content = listOf(student("s1", "A", "12"), student("s2", "B", "01")),
            last = false,
        )
        coEvery { api.students(1, 100, studentClass = "5") } returns PageResponse(
            content = listOf(student("s3", "a", "03")),
            last = true,
        )

        val result = repository.allInClass("5", "A") as Outcome.Success

        // Section matching is case-insensitive and results are sorted by roll number.
        assertEquals(listOf("s3", "s1"), result.data.map { it.id })
    }

    @Test
    fun `allInClass with no matching section returns an empty list`() = runTest {
        coEvery { api.students(0, 100, studentClass = "5") } returns PageResponse(
            content = listOf(student("s1", "B", "01")),
            last = true,
        )

        val result = repository.allInClass("5", "A") as Outcome.Success

        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `allInClass offline is a network failure`() = runTest {
        coEvery { api.students(any(), any(), studentClass = "5") } throws IOException("offline")

        val result = repository.allInClass("5", "A")

        assertTrue(result is Outcome.Failure && result.isNetwork)
    }

    @Test
    fun `allInClass stops when a page comes back empty`() = runTest {
        coEvery { api.students(0, 100, studentClass = "5") } returns PageResponse(
            content = emptyList(),
            last = false, // defensive: server says more pages but returns nothing
        )

        val result = repository.allInClass("5", "A") as Outcome.Success

        assertTrue(result.data.isEmpty())
    }
}
