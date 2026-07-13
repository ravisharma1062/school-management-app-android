package com.school.app.data.repository

import android.content.Context
import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.SubscriptionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamResultRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    suspend fun forStudent(studentId: String): Outcome<List<ExamResult>> =
        safeApiCall { api.examResults(studentId) }

    /** Downloads the report card PDF into the app's cache dir, ready to share via FileProvider. */
    suspend fun downloadReportCard(studentId: String): Outcome<File> = safeApiCall {
        val body = api.reportCard(studentId)
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val file = File(dir, "report-card-$studentId.pdf")
            body.byteStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }
    }
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

/** GET /subscription is ADMIN-only per the backend contract — callers should expect this to
 * fail for other roles and treat that as "no entitlement data available", not an error to show. */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun getCurrent(): Outcome<SubscriptionDto> = safeApiCall { api.subscription() }
}
