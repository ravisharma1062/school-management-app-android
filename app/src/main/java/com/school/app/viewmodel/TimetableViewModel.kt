package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.repository.TimetableRepository
import com.school.app.domain.model.TimetableEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val timetableRepository: TimetableRepository,
) : ViewModel() {

    var studentClass by mutableStateOf(savedStateHandle["cls"] ?: "")
    var section by mutableStateOf(savedStateHandle["sec"] ?: "")

    var state by mutableStateOf<UiState<List<TimetableEntry>>?>(null)
        private set

    init {
        if (studentClass.isNotBlank() && section.isNotBlank()) load()
    }

    fun load() {
        val cls = studentClass.trim()
        val sec = section.trim()
        if (cls.isEmpty() || sec.isEmpty()) {
            state = UiState.Error("Enter class and section")
            return
        }
        state = UiState.Loading
        viewModelScope.launch {
            state = timetableRepository.forClass(cls, sec).toUiState()
        }
    }
}
