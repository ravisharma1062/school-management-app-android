package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.LibraryRepository
import com.school.app.domain.model.Book
import com.school.app.domain.model.BookIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<BookIssue>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = libraryRepository.getIssuesForStudent(studentId).toUiState()
        }
    }
}

/** Paged, searchable book catalog — available to all roles. */
@HiltViewModel
class LibraryCatalogViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    data class State(
        val items: List<Book> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null,
        val page: Int = 0,
    )

    var state by mutableStateOf(State())
        private set

    var search by mutableStateOf("")

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
            val result = libraryRepository.searchBooks(page = page, search = search.trim().ifBlank { null })
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
