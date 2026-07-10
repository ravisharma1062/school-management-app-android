package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.describeHttpError
import com.school.app.data.local.AttendanceDao
import com.school.app.data.local.CachedAttendance
import com.school.app.data.local.PendingAttendance
import com.school.app.data.local.PendingAttendanceDao
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceMarkRequest
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val api: ApiService,
    private val attendanceDao: AttendanceDao,
    private val pendingDao: PendingAttendanceDao,
) {
    /** Number of attendance marks queued locally, waiting for connectivity. */
    val pendingCount: Flow<Int> = pendingDao.count()

    /** Attendance history for one student; falls back to the Room cache offline. */
    suspend fun history(studentId: String): Outcome<List<Attendance>> {
        val remote = safeApiCall { api.attendanceForStudent(studentId) }
        return when (remote) {
            is Outcome.Success -> {
                attendanceDao.replaceForStudent(studentId, remote.data.map(CachedAttendance::from))
                remote
            }
            is Outcome.Failure -> {
                if (!remote.isNetwork) return remote
                val cached = attendanceDao.forStudent(studentId)
                if (cached.isEmpty()) remote
                else Outcome.Success(cached.map { it.toModel() }, fromCache = true)
            }
        }
    }

    /** Existing marks for a class on a date, used to prefill the marking grid. */
    suspend fun classOnDate(
        studentClass: String,
        section: String,
        date: String,
    ): Outcome<List<Attendance>> =
        safeApiCall { api.attendanceForClass(studentClass, section, date) }

    data class SubmitResult(
        val sent: Int,
        val queued: Int,
        val failures: List<Pair<String, String>>, // student name -> error
    )

    /**
     * Submits marks one by one. The first connectivity error queues that mark and
     * every remaining one into Room for [syncPending] to flush later; HTTP errors
     * (e.g. already marked) are reported per student but don't stop the batch.
     */
    suspend fun submit(
        marks: List<AttendanceMarkRequest>,
        studentNames: Map<String, String>,
    ): SubmitResult {
        var sent = 0
        val failures = mutableListOf<Pair<String, String>>()
        val remaining = marks.toMutableList()

        while (remaining.isNotEmpty()) {
            val mark = remaining.first()
            try {
                api.markAttendance(mark)
                sent++
                remaining.removeAt(0)
            } catch (e: HttpException) {
                failures += (studentNames[mark.studentId] ?: mark.studentId) to describeHttpError(e)
                remaining.removeAt(0)
            } catch (e: IOException) {
                // Offline: queue everything that hasn't been sent yet.
                enqueue(remaining, studentNames)
                return SubmitResult(sent, remaining.size, failures)
            }
        }
        return SubmitResult(sent, 0, failures)
    }

    private suspend fun enqueue(
        marks: List<AttendanceMarkRequest>,
        studentNames: Map<String, String>,
    ) {
        val now = System.currentTimeMillis()
        pendingDao.insertAll(
            marks.map {
                PendingAttendance(
                    studentId = it.studentId,
                    studentName = studentNames[it.studentId] ?: "",
                    date = it.date,
                    status = it.status.name,
                    queuedAt = now,
                )
            },
        )
    }

    data class SyncResult(val synced: Int, val dropped: Int, val stillPending: Int)

    /**
     * Flushes the offline queue. HTTP rejections (duplicate mark, validation) are
     * permanent, so those rows are dropped; a connectivity error stops the pass
     * and leaves the rest queued for the next reconnect.
     */
    suspend fun syncPending(): SyncResult {
        var synced = 0
        var dropped = 0
        val rows = pendingDao.all()
        for ((index, row) in rows.withIndex()) {
            try {
                api.markAttendance(
                    AttendanceMarkRequest(
                        studentId = row.studentId,
                        date = row.date,
                        status = enumValueOf(row.status),
                    ),
                )
                pendingDao.delete(row)
                synced++
            } catch (e: HttpException) {
                pendingDao.delete(row)
                dropped++
            } catch (e: IOException) {
                pendingDao.setError(row.localId, "Offline; will retry")
                return SyncResult(synced, dropped, rows.size - index)
            }
        }
        return SyncResult(synced, dropped, 0)
    }
}
