package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.local.CachedHomework
import com.school.app.data.local.HomeworkDao
import com.school.app.data.remote.ApiService
import com.school.app.data.repository.HomeworkRepository
import com.school.app.domain.model.Homework
import com.school.app.domain.model.PageResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HomeworkRepositoryTest {

    private val api = mockk<ApiService>()
    private val dao = mockk<HomeworkDao>(relaxed = true)
    private val repository = HomeworkRepository(api, dao)

    private val homework = Homework(
        id = "h1", studentClass = "5", section = "A", subject = "Maths",
        title = "Fractions", description = null, dueDate = "2026-07-20",
        createdBy = "t1", createdAt = "2026-07-16T09:00:00Z",
    )

    @Test
    fun `page zero refreshes the offline cache`() = runTest {
        coEvery { api.homework("5", "A", 0, 20) } returns PageResponse(content = listOf(homework))

        val result = repository.page("5", "A", page = 0)

        assertTrue(result is Outcome.Success)
        coVerify { dao.replaceForClass("5", "A", listOf(CachedHomework.from(homework))) }
    }

    @Test
    fun `later pages do not rewrite the cache`() = runTest {
        coEvery { api.homework("5", "A", 1, 20) } returns PageResponse(content = listOf(homework))

        repository.page("5", "A", page = 1)

        coVerify(exactly = 0) { dao.replaceForClass(any(), any(), any()) }
    }

    @Test
    fun `offline on page zero serves the cache as a single page`() = runTest {
        coEvery { api.homework("5", "A", 0, 20) } throws IOException("offline")
        coEvery { dao.forClass("5", "A") } returns listOf(CachedHomework.from(homework))

        val result = repository.page("5", "A", page = 0) as Outcome.Success

        assertTrue(result.fromCache)
        assertEquals(listOf(homework), result.data.content)
        assertEquals(1, result.data.totalPages)
        assertTrue(result.data.first && result.data.last)
    }

    @Test
    fun `offline on a later page fails instead of serving stale page zero data`() = runTest {
        coEvery { api.homework("5", "A", 2, 20) } throws IOException("offline")

        val result = repository.page("5", "A", page = 2)

        assertTrue(result is Outcome.Failure && result.isNetwork)
        coVerify(exactly = 0) { dao.forClass(any(), any()) }
    }
}
