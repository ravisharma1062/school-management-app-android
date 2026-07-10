package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.ExamResultRepository
import com.school.app.data.repository.FeeRepository
import com.school.app.data.repository.NoticeRepository
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Notice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val examResultRepository: ExamResultRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<ExamResult>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = examResultRepository.forStudent(studentId).toUiState()
        }
    }
}

@HiltViewModel
class FeesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val feeRepository: FeeRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<Fee>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = feeRepository.forStudent(studentId).toUiState()
        }
    }
}

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository,
) : ViewModel() {

    data class State(
        val items: List<Notice> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
    )

    var state by mutableStateOf(State())
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
            when (val result = noticeRepository.page(page)) {
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
}
