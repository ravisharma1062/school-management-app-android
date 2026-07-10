package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.Student
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun page(page: Int, size: Int = 20): Outcome<PageResponse<Student>> =
        safeApiCall { api.students(page, size) }

    suspend fun byId(id: String): Outcome<Student> = safeApiCall { api.student(id) }

    suspend fun myChildren(): Outcome<List<Student>> = safeApiCall { api.myChildren() }

    /**
     * Students of one class+section. The API has no class filter, so this pages
     * through the directory and filters client-side (same as the web app).
     */
    suspend fun allInClass(studentClass: String, section: String): Outcome<List<Student>> =
        safeApiCall {
            val all = mutableListOf<Student>()
            var page = 0
            while (true) {
                val res = api.students(page, 100)
                all += res.content
                if (res.last || res.content.isEmpty() || page >= 50) break
                page++
            }
            all.filter {
                it.studentClass.equals(studentClass, ignoreCase = true) &&
                    it.section.equals(section, ignoreCase = true)
            }.sortedBy { it.rollNo }
        }
}
