package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.EventCreateRequest
import com.school.app.domain.model.EventRsvpDto
import com.school.app.domain.model.EventRsvpRequest
import com.school.app.domain.model.RsvpStatus
import com.school.app.domain.model.SchoolEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun list(range: Int = 90): Outcome<List<SchoolEvent>> = safeApiCall { api.events(range) }

    suspend fun create(request: EventCreateRequest): Outcome<SchoolEvent> = safeApiCall { api.createEvent(request) }

    suspend fun rsvp(eventId: String, status: RsvpStatus): Outcome<EventRsvpDto> =
        safeApiCall { api.rsvpEvent(eventId, EventRsvpRequest(status)) }

    suspend fun rsvps(eventId: String): Outcome<List<EventRsvpDto>> = safeApiCall { api.eventRsvps(eventId) }
}
