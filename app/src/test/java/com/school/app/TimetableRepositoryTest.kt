package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.local.CachedTimetableEntry
import com.school.app.data.local.TimetableDao
import com.school.app.data.remote.ApiService
import com.school.app.data.repository.TimetableRepository
import com.school.app.domain.model.TimetableEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class TimetableRepositoryTest {

    private val api = mockk<ApiService>()
    private val dao = mockk<TimetableDao>(relaxed = true)
    private val repository = TimetableRepository(api, dao)

    private val entry = TimetableEntry("tt1", "5", "A", "MONDAY", 1, "Maths", "t1")

    @Test
    fun `success refreshes the cache for that class`() = runTest {
        coEvery { api.timetable("5", "A") } returns listOf(entry)

        val result = repository.forClass("5", "A")

        assertEquals(Outcome.Success(listOf(entry)), result)
        coVerify { dao.replaceForClass("5", "A", listOf(CachedTimetableEntry.from(entry))) }
    }

    @Test
    fun `offline serves the cached timetable`() = runTest {
        coEvery { api.timetable("5", "A") } throws IOException("offline")
        coEvery { dao.forClass("5", "A") } returns listOf(CachedTimetableEntry.from(entry))

        val result = repository.forClass("5", "A") as Outcome.Success

        assertTrue(result.fromCache)
        assertEquals(listOf(entry), result.data)
    }

    @Test
    fun `offline with no cache surfaces the network failure`() = runTest {
        coEvery { api.timetable("5", "A") } throws IOException("offline")
        coEvery { dao.forClass("5", "A") } returns emptyList()

        val result = repository.forClass("5", "A")

        assertTrue(result is Outcome.Failure && result.isNetwork)
    }

    @Test
    fun `http failure is returned without touching the cache`() = runTest {
        val body = """{"message":"Forbidden"}""".toResponseBody("application/json".toMediaType())
        coEvery { api.timetable("5", "A") } throws HttpException(Response.error<Any>(403, body))

        val result = repository.forClass("5", "A") as Outcome.Failure

        assertEquals("Forbidden", result.message)
        coVerify(exactly = 0) { dao.forClass(any(), any()) }
    }
}
