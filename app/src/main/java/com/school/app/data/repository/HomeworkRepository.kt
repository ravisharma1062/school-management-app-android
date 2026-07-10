package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.local.CachedHomework
import com.school.app.data.local.HomeworkDao
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkCreateRequest
import com.school.app.domain.model.PageResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeworkRepository @Inject constructor(
    private val api: ApiService,
    private val homeworkDao: HomeworkDao,
) {
    /**
     * One page of homework for a class+section. Page 0 refreshes the offline
     * cache; when offline, the cache is served back as a single page.
     */
    suspend fun page(
        studentClass: String,
        section: String,
        page: Int,
        size: Int = 20,
    ): Outcome<PageResponse<Homework>> {
        val remote = safeApiCall { api.homework(studentClass, section, page, size) }
        return when (remote) {
            is Outcome.Success -> {
                if (page == 0) {
                    homeworkDao.replaceForClass(
                        studentClass,
                        section,
                        remote.data.content.map(CachedHomework::from),
                    )
                }
                remote
            }
            is Outcome.Failure -> {
                if (!remote.isNetwork || page > 0) return remote
                val cached = homeworkDao.forClass(studentClass, section)
                if (cached.isEmpty()) remote
                else Outcome.Success(
                    PageResponse(
                        content = cached.map { it.toModel() },
                        totalElements = cached.size.toLong(),
                        totalPages = 1,
                        size = cached.size,
                        number = 0,
                        first = true,
                        last = true,
                    ),
                    fromCache = true,
                )
            }
        }
    }

    suspend fun create(request: HomeworkCreateRequest): Outcome<Homework> =
        safeApiCall { api.createHomework(request) }
}
