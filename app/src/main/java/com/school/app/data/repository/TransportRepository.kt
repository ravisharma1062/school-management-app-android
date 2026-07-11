package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.BusLocation
import com.school.app.domain.model.StudentTransport
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransportRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun getStudentTransport(studentId: String): Outcome<StudentTransport> =
        safeApiCall { api.studentTransport(studentId) }

    suspend fun getBusLocation(routeId: String): Outcome<BusLocation> =
        safeApiCall { api.busLocation(routeId) }
}
