package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.EventRepository
import com.school.app.domain.model.EventCreateRequest
import com.school.app.domain.model.EventRsvpDto
import com.school.app.domain.model.RsvpStatus
import com.school.app.domain.model.SchoolEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val RANGE_DAYS = 90

@HiltViewModel
class EventsListViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel() {

    data class State(
        val items: List<SchoolEvent> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
    )

    var state by mutableStateOf(State())
        private set

    var rsvpingEventId by mutableStateOf<String?>(null)
        private set

    val expandedRsvpsEventIds: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    val rsvpsByEventId: SnapshotStateMap<String, List<EventRsvpDto>> = mutableStateMapOf()

    init {
        refresh()
    }

    fun refresh() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            state = when (val result = eventRepository.list(RANGE_DAYS)) {
                is Outcome.Success -> State(items = result.data)
                is Outcome.Failure -> State(error = result.message)
            }
        }
    }

    fun rsvp(eventId: String, status: RsvpStatus) {
        if (rsvpingEventId != null) return
        rsvpingEventId = eventId
        viewModelScope.launch {
            when (val result = eventRepository.rsvp(eventId, status)) {
                is Outcome.Success -> refresh()
                is Outcome.Failure -> state = state.copy(error = result.message)
            }
            rsvpingEventId = null
        }
    }

    fun toggleRsvps(eventId: String) {
        if (expandedRsvpsEventIds.remove(eventId) == null) {
            expandedRsvpsEventIds[eventId] = true
            viewModelScope.launch {
                when (val result = eventRepository.rsvps(eventId)) {
                    is Outcome.Success -> rsvpsByEventId[eventId] = result.data
                    is Outcome.Failure -> Unit
                }
            }
        }
    }
}

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val eventRepository: EventRepository,
) : ViewModel() {

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf("")
    var eventDate by mutableStateOf(LocalDate.now().plusDays(1))

    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var created by mutableStateOf(false)
        private set

    fun submit() {
        if (submitting) return
        if (title.isBlank()) {
            error = "Title is required"
            return
        }
        submitting = true
        error = null
        viewModelScope.launch {
            val result = eventRepository.create(
                EventCreateRequest(
                    title = title.trim(),
                    description = description.trim().ifEmpty { null },
                    eventDate = eventDate.toString(),
                    location = location.trim().ifEmpty { null },
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
