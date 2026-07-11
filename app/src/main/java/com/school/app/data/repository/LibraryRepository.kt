package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.Book
import com.school.app.domain.model.BookIssue
import com.school.app.domain.model.PageResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun getIssuesForStudent(studentId: String): Outcome<List<BookIssue>> =
        safeApiCall { api.libraryIssuesForStudent(studentId) }

    suspend fun searchBooks(page: Int, size: Int = 20, search: String? = null): Outcome<PageResponse<Book>> =
        safeApiCall { api.libraryBooks(page, size, search) }
}
