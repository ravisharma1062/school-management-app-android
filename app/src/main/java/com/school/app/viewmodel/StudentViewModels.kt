package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.StudentRepository
import com.school.app.domain.model.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Paged student directory for teachers/admins. */
@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    data class State(
        val items: List<Student> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
    )

    var state by mutableStateOf(State())
        private set

    var nameFilter by mutableStateOf("")
    var rollNoFilter by mutableStateOf("")
    var classFilter by mutableStateOf("")

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
            val result = studentRepository.page(
                page = page,
                name = nameFilter.trim().ifBlank { null },
                rollNo = rollNoFilter.trim().ifBlank { null },
                studentClass = classFilter.trim().ifBlank { null },
            )
            when (result) {
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

/** Parent's children, from GET /students/my-children. */
@HiltViewModel
class ChildrenViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<Student>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = studentRepository.myChildren().toUiState()
        }
    }
}

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])

    var state by mutableStateOf<UiState<Student>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = studentRepository.byId(studentId).toUiState()
        }
    }
}
