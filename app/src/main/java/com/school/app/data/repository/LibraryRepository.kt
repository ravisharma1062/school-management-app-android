package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.BookIssue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun getIssuesForStudent(studentId: String): Outcome<List<BookIssue>> =
        safeApiCall { api.libraryIssuesForStudent(studentId) }
}
