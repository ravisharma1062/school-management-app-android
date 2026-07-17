package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.repository.EventRepository
import com.school.app.domain.model.EventRsvpDto
import com.school.app.domain.model.RsvpStatus
import com.school.app.domain.model.SchoolEvent
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.EventCreateViewModel
import com.school.app.viewmodel.EventsListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<EventRepository>()

    private fun event(id: String) =
        SchoolEvent(id, "Sports day", null, "2026-08-01", null, "admin1", "2026-07-16T00:00:00Z")

    @Test
    fun `refresh loads the event list on init`() = runTest {
        coEvery { repository.list(90) } returns Outcome.Success(listOf(event("e1")))

        val vm = EventsListViewModel(repository)

        assertEquals(listOf("e1"), vm.state.items.map { it.id })
        assertFalse(vm.state.loading)
    }

    @Test
    fun `a failed refresh clears any prior items and shows the error`() = runTest {
        coEvery { repository.list(90) } returns Outcome.Failure("Network down")

        val vm = EventsListViewModel(repository)

        assertTrue(vm.state.items.isEmpty())
        assertEquals("Network down", vm.state.error)
    }

    @Test
    fun `toggleRsvps expands and loads on first call, collapses on the second without refetching`() = runTest {
        coEvery { repository.list(90) } returns Outcome.Success(emptyList())
        coEvery { repository.rsvps("e1") } returns Outcome.Success(
            listOf(EventRsvpDto("r1", "e1", "u1", "Asha", RsvpStatus.GOING, "2026-07-16T00:00:00Z")),
        )
        val vm = EventsListViewModel(repository)

        vm.toggleRsvps("e1")
        assertTrue(vm.expandedRsvpsEventIds.containsKey("e1"))
        assertEquals(1, vm.rsvpsByEventId["e1"]?.size)

        vm.toggleRsvps("e1")
        assertFalse(vm.expandedRsvpsEventIds.containsKey("e1"))
        coVerify(exactly = 1) { repository.rsvps("e1") }
    }

    @Test
    fun `rsvp guards against a second call while one is in flight`() = runTest {
        coEvery { repository.list(90) } returns Outcome.Success(listOf(event("e1")))
        val vm = EventsListViewModel(repository)
        val gate = kotlinx.coroutines.CompletableDeferred<Outcome<com.school.app.domain.model.EventRsvpDto>>()
        coEvery { repository.rsvp("e1", RsvpStatus.GOING) } coAnswers { gate.await() }

        vm.rsvp("e1", RsvpStatus.GOING)
        assertEquals("e1", vm.rsvpingEventId)
        vm.rsvp("e1", RsvpStatus.MAYBE)

        gate.complete(Outcome.Success(EventRsvpDto("r1", "e1", "u1", "Asha", RsvpStatus.GOING, "2026-07-16T00:00:00Z")))

        coVerify(exactly = 1) { repository.rsvp(any(), any()) }
    }

    @Test
    fun `create rejects a blank title without calling the repository`() = runTest {
        val vm = EventCreateViewModel(repository)
        vm.title = "   "

        vm.submit()

        assertEquals("Title is required", vm.error)
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `create trims blank optional fields down to null`() = runTest {
        val vm = EventCreateViewModel(repository)
        vm.title = "Sports day"
        vm.description = "   "
        vm.location = "  "
        coEvery { repository.create(match { it.description == null && it.location == null }) } returns
            Outcome.Success(event("e1"))

        vm.submit()

        assertTrue(vm.created)
        assertFalse(vm.submitting)
    }
}
