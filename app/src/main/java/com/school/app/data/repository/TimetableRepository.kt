package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.local.CachedTimetableEntry
import com.school.app.data.local.TimetableDao
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.TimetableEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    private val api: ApiService,
    private val timetableDao: TimetableDao,
) {
    /** Timetable for a class+section; falls back to the Room cache offline. */
    suspend fun forClass(studentClass: String, section: String): Outcome<List<TimetableEntry>> {
        val remote = safeApiCall { api.timetable(studentClass, section) }
        return when (remote) {
            is Outcome.Success -> {
                timetableDao.replaceForClass(
                    studentClass,
                    section,
                    remote.data.map(CachedTimetableEntry::from),
                )
                remote
            }
            is Outcome.Failure -> {
                if (!remote.isNetwork) return remote
                val cached = timetableDao.forClass(studentClass, section)
                if (cached.isEmpty()) remote
                else Outcome.Success(cached.map { it.toModel() }, fromCache = true)
            }
        }
    }
}
