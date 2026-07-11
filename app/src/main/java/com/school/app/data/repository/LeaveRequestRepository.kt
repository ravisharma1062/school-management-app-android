package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.LeaveRequest
import com.school.app.domain.model.LeaveRequestCreateRequest
import com.school.app.domain.model.LeaveRequestReviewRequest
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.PageResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRequestRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun page(page: Int, size: Int = 20, status: LeaveStatus? = null): Outcome<PageResponse<LeaveRequest>> =
        safeApiCall { api.leaveRequests(page, size, status?.name) }

    suspend fun create(request: LeaveRequestCreateRequest): Outcome<LeaveRequest> =
        safeApiCall { api.createLeaveRequest(request) }

    suspend fun review(id: String, status: LeaveStatus): Outcome<LeaveRequest> =
        safeApiCall { api.reviewLeaveRequest(id, LeaveRequestReviewRequest(status)) }
}
