package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PageResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamResultRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun forStudent(studentId: String): Outcome<List<ExamResult>> =
        safeApiCall { api.examResults(studentId) }
}

@Singleton
class NoticeRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun page(page: Int, size: Int = 20): Outcome<PageResponse<Notice>> =
        safeApiCall { api.notices(page, size) }
}

@Singleton
class FeeRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun forStudent(studentId: String): Outcome<List<Fee>> =
        safeApiCall { api.feesForStudent(studentId) }
}
