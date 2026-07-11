package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.TransportRepository
import com.school.app.domain.model.BusLocation
import com.school.app.domain.model.StudentTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 15_000L

@HiltViewModel
class TransportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transportRepository: TransportRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var assignment by mutableStateOf<StudentTransport?>(null)
        private set
    var location by mutableStateOf<BusLocation?>(null)
        private set

    init {
        load()
    }

    fun load() {
        loading = true
        error = null
        viewModelScope.launch {
            when (val result = transportRepository.getStudentTransport(studentId)) {
                is Outcome.Success -> {
                    assignment = result.data
                    loading = false
                    pollLocation(result.data.routeId)
                }
                is Outcome.Failure -> {
                    assignment = null
                    loading = false
                    error = result.message
                }
            }
        }
    }

    private fun pollLocation(routeId: String) {
        viewModelScope.launch {
            while (true) {
                when (val result = transportRepository.getBusLocation(routeId)) {
                    is Outcome.Success -> location = result.data
                    is Outcome.Failure -> Unit
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
}
