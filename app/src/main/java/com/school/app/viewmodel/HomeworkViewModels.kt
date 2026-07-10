package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.HomeworkRepository
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkCreateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeworkListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val homeworkRepository: HomeworkRepository,
) : ViewModel() {

    var studentClass by mutableStateOf(savedStateHandle["cls"] ?: "")
    var section by mutableStateOf(savedStateHandle["sec"] ?: "")

    data class State(
        val items: List<Homework> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = true,
        val fromCache: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
        val loadedFor: Pair<String, String>? = null,
    )

    var state by mutableStateOf(State())
        private set

    init {
        if (studentClass.isNotBlank() && section.isNotBlank()) refresh()
    }

    fun refresh() {
        val cls = studentClass.trim()
        val sec = section.trim()
        if (cls.isEmpty() || sec.isEmpty()) {
            state = State(error = "Enter class and section")
            return
        }
        state = State(loading = true)
        loadPage(cls, sec, 0)
    }

    fun loadNext() {
        val target = state.loadedFor ?: return
        if (state.loading || state.loadingMore || state.endReached) return
        state = state.copy(loadingMore = true)
        loadPage(target.first, target.second, state.page + 1)
    }

    private fun loadPage(cls: String, sec: String, page: Int) {
        viewModelScope.launch {
            when (val result = homeworkRepository.page(cls, sec, page)) {
                is Outcome.Success -> state = state.copy(
                    items = if (page == 0) result.data.content else state.items + result.data.content,
                    loading = false,
                    loadingMore = false,
                    endReached = result.data.last,
                    fromCache = result.fromCache,
                    error = null,
                    page = page,
                    loadedFor = cls to sec,
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

@HiltViewModel
class HomeworkCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val homeworkRepository: HomeworkRepository,
) : ViewModel() {

    var studentClass by mutableStateOf(savedStateHandle["cls"] ?: "")
    var section by mutableStateOf(savedStateHandle["sec"] ?: "")
    var subject by mutableStateOf("")
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var dueDate by mutableStateOf(LocalDate.now().plusDays(1))

    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var created by mutableStateOf(false)
        private set

    fun submit() {
        if (submitting) return
        if (studentClass.isBlank() || section.isBlank() || subject.isBlank() || title.isBlank()) {
            error = "Class, section, subject and title are required"
            return
        }
        submitting = true
        error = null
        viewModelScope.launch {
            val result = homeworkRepository.create(
                HomeworkCreateRequest(
                    studentClass = studentClass.trim(),
                    section = section.trim(),
                    subject = subject.trim(),
                    title = title.trim(),
                    description = description.trim().ifEmpty { null },
                    dueDate = dueDate.toString(),
                ),
            )
            when (result) {
                is Outcome.Success -> created = true
                is Outcome.Failure -> error = result.message
            }
            submitting = false
        }
    }
}
