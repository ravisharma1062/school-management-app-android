package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.local.AttendanceDao
import com.school.app.data.local.CachedAttendance
import com.school.app.data.local.PendingAttendance
import com.school.app.data.local.PendingAttendanceDao
import com.school.app.data.remote.ApiService
import com.school.app.data.repository.AttendanceRepository
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceMarkRequest
import com.school.app.domain.model.AttendanceStatus
import io.mockk.coEvery
import io.mockk.coJustRun
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

class AttendanceRepositoryTest {

    private val api = mockk<ApiService>()
    private val attendanceDao = mockk<AttendanceDao>(relaxed = true)
    private val pendingDao = mockk<PendingAttendanceDao>(relaxed = true)
    private val repository = AttendanceRepository(api, attendanceDao, pendingDao)

    private val mark = Attendance("a1", "s1", "2026-07-16", AttendanceStatus.PRESENT, "t1")

    private fun httpException(code: Int, message: String = "err"): HttpException =
        HttpException(
            Response.error<Any>(
                code,
                """{"message":"$message"}""".toResponseBody("application/json".toMediaType()),
            ),
        )

    // --- history ---

    @Test
    fun `history success refreshes the cache and returns fresh data`() = runTest {
        coEvery { api.attendanceForStudent("s1") } returns listOf(mark)

        val result = repository.history("s1")

        assertEquals(Outcome.Success(listOf(mark)), result)
        coVerify { attendanceDao.replaceForStudent("s1", listOf(CachedAttendance.from(mark))) }
    }

    @Test
    fun `history offline falls back to the cache`() = runTest {
        coEvery { api.attendanceForStudent("s1") } throws IOException("offline")
        coEvery { attendanceDao.forStudent("s1") } returns listOf(CachedAttendance.from(mark))

        val result = repository.history("s1") as Outcome.Success

        assertTrue(result.fromCache)
        assertEquals(listOf(mark), result.data)
    }

    @Test
    fun `history offline with an empty cache surfaces the network failure`() = runTest {
        coEvery { api.attendanceForStudent("s1") } throws IOException("offline")
        coEvery { attendanceDao.forStudent("s1") } returns emptyList()

        val result = repository.history("s1")

        assertTrue(result is Outcome.Failure && result.isNetwork)
    }

    @Test
    fun `history http failure does not consult the cache`() = runTest {
        coEvery { api.attendanceForStudent("s1") } throws httpException(404, "Student not found")

        val result = repository.history("s1") as Outcome.Failure

        assertEquals("Student not found", result.message)
        coVerify(exactly = 0) { attendanceDao.forStudent(any()) }
    }

    // --- submit ---

    private fun requests(vararg ids: String) =
        ids.map { AttendanceMarkRequest(it, "2026-07-16", AttendanceStatus.PRESENT) }

    private val names = mapOf("s1" to "Asha", "s2" to "Bilal", "s3" to "Chitra")

    @Test
    fun `submit sends every mark when all succeed`() = runTest {
        coEvery { api.markAttendance(any()) } returns mark

        val result = repository.submit(requests("s1", "s2", "s3"), names)

        assertEquals(AttendanceRepository.SubmitResult(sent = 3, queued = 0, failures = emptyList()), result)
        coVerify(exactly = 0) { pendingDao.insertAll(any()) }
    }

    @Test
    fun `submit reports an http rejection per student and continues the batch`() = runTest {
        coEvery { api.markAttendance(match { it.studentId == "s2" }) } throws
            httpException(409, "Already marked")
        coEvery { api.markAttendance(match { it.studentId != "s2" }) } returns mark

        val result = repository.submit(requests("s1", "s2", "s3"), names)

        assertEquals(2, result.sent)
        assertEquals(0, result.queued)
        assertEquals(listOf("Bilal" to "Already marked"), result.failures)
    }

    @Test
    fun `submit going offline queues the unsent remainder`() = runTest {
        coEvery { api.markAttendance(match { it.studentId == "s1" }) } returns mark
        coEvery { api.markAttendance(match { it.studentId == "s2" }) } throws IOException("offline")

        val result = repository.submit(requests("s1", "s2", "s3"), names)

        assertEquals(1, result.sent)
        assertEquals(2, result.queued)
        coVerify {
            pendingDao.insertAll(
                match { rows ->
                    rows.map { it.studentId } == listOf("s2", "s3") &&
                        rows.all { it.status == "PRESENT" && it.date == "2026-07-16" } &&
                        rows.map { it.studentName } == listOf("Bilal", "Chitra")
                },
            )
        }
    }

    // --- syncPending ---

    private fun pendingRow(id: Long, studentId: String) = PendingAttendance(
        localId = id, studentId = studentId, studentName = "n$studentId",
        date = "2026-07-16", status = "ABSENT", queuedAt = 1L,
    )

    @Test
    fun `syncPending drops permanent rejections and keeps counting`() = runTest {
        val rows = listOf(pendingRow(1, "s1"), pendingRow(2, "s2"), pendingRow(3, "s3"))
        coEvery { pendingDao.all() } returns rows
        coJustRun { pendingDao.delete(any()) }
        coEvery { api.markAttendance(match { it.studentId == "s2" }) } throws httpException(409)
        coEvery { api.markAttendance(match { it.studentId != "s2" }) } returns mark

        val result = repository.syncPending()

        assertEquals(AttendanceRepository.SyncResult(synced = 2, dropped = 1, stillPending = 0), result)
        coVerify(exactly = 3) { pendingDao.delete(any()) }
    }

    @Test
    fun `syncPending stops on a connectivity error and leaves the rest queued`() = runTest {
        val rows = listOf(pendingRow(1, "s1"), pendingRow(2, "s2"), pendingRow(3, "s3"))
        coEvery { pendingDao.all() } returns rows
        coJustRun { pendingDao.delete(any()) }
        coEvery { api.markAttendance(match { it.studentId == "s1" }) } returns mark
        coEvery { api.markAttendance(match { it.studentId == "s2" }) } throws IOException("offline")

        val result = repository.syncPending()

        assertEquals(AttendanceRepository.SyncResult(synced = 1, dropped = 0, stillPending = 2), result)
        coVerify { pendingDao.setError(2L, "Offline; will retry") }
        coVerify(exactly = 1) { pendingDao.delete(any()) }
    }

    @Test
    fun `syncPending with an empty queue does nothing`() = runTest {
        coEvery { pendingDao.all() } returns emptyList()

        val result = repository.syncPending()

        assertEquals(AttendanceRepository.SyncResult(0, 0, 0), result)
    }
}
