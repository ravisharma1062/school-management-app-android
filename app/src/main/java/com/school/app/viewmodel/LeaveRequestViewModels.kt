package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.LeaveRequestRepository
import com.school.app.domain.model.LeaveRequest
import com.school.app.domain.model.LeaveRequestCreateRequest
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.LeaveType
import com.school.app.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LeaveRequestsViewModel @Inject constructor(
    private val leaveRequestRepository: LeaveRequestRepository,
) : ViewModel() {

    data class State(
        val items: List<LeaveRequest> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
    )

    var state by mutableStateOf(State())
        private set

    var statusFilter by mutableStateOf<LeaveStatus?>(null)

    // --- Submission form (TEACHER/PARENT) ---
    var type by mutableStateOf(LeaveType.CASUAL)
    var fromDate by mutableStateOf(LocalDate.now())
    var toDate by mutableStateOf(LocalDate.now())
    var reason by mutableStateOf("")
    var submitting by mutableStateOf(false)
        private set
    var submitError by mutableStateOf<String?>(null)
        private set
    var submitSuccess by mutableStateOf(false)
        private set

    // --- Review (ADMIN) ---
    var reviewingId by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        state = State(loading = true)
        loadPage(0)
    }

    fun loadNext() {
        if (state.loading || state.loadingMore || state.endReached) return
        state = state.copy(loadingMore = true)
        loadPage(state.page + 1)
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            when (val result = leaveRequestRepository.page(page, status = statusFilter)) {
                is Outcome.Success -> state = state.copy(
                    items = if (page == 0) result.data.content else state.items + result.data.content,
                    loading = false,
                    loadingMore = false,
                    endReached = result.data.last,
                    error = null,
                    page = page,
                )
                is Outcome.Failure -> state = state.copy(
                    loading = false,
                    loadingMore = false,
                    error = result.message,
                )
            }
        }
    }

    fun submit() {
        if (submitting) return
        if (toDate.isBefore(fromDate)) {
            submitError = "End date must not be before start date"
            return
        }
        submitting = true
        submitError = null
        submitSuccess = false
        viewModelScope.launch {
            val result = leaveRequestRepository.create(
                LeaveRequestCreateRequest(
                    type = type,
                    fromDate = fromDate.toString(),
                    toDate = toDate.toString(),
                    reason = reason.trim().ifBlank { null },
                ),
            )
            when (result) {
                is Outcome.Success -> {
                    submitSuccess = true
                    reason = ""
                    refresh()
                }
                is Outcome.Failure -> submitError = result.message
            }
            submitting = false
        }
    }

    fun review(id: String, status: LeaveStatus) {
        if (reviewingId != null) return
        reviewingId = id
        viewModelScope.launch {
            leaveRequestRepository.review(id, status)
            reviewingId = null
            refresh()
        }
    }
}

fun canReviewLeaveRequests(role: Role) = role == Role.ADMIN
