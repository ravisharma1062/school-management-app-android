package com.school.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.LibraryRepository
import com.school.app.data.repository.StudentRepository
import com.school.app.data.repository.TransportRepository
import com.school.app.domain.model.Book
import com.school.app.domain.model.BookIssue
import com.school.app.domain.model.BookIssueStatus
import com.school.app.domain.model.BusLocation
import com.school.app.domain.model.Student
import com.school.app.domain.model.StudentTransport
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.LibraryViewModel
import com.school.app.viewmodel.StudentDetailViewModel
import com.school.app.viewmodel.TransportViewModel
import com.school.app.viewmodel.UiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the ViewModels that take a SavedStateHandle for nav-arg-derived state
 * (studentId/name), confirming androidx.lifecycle.SavedStateHandle's map constructor
 * works on the plain JVM without Robolectric, plus each ViewModel's load logic.
 */
class SavedStateHandleViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- StudentDetailViewModel ---

    @Test
    fun `student detail requires a studentId in the saved state`() {
        val repository = mockk<StudentRepository>()
        assertThrows(IllegalStateException::class.java) {
            StudentDetailViewModel(SavedStateHandle(mapOf()), repository)
        }
    }

    @Test
    fun `student detail loads the student by id from the saved state`() = runTest {
        val repository = mockk<StudentRepository>()
        val student = Student("s1", "Asha", "12", "5", "A", "2015-01-01")
        coEvery { repository.byId("s1") } returns Outcome.Success(student)

        val vm = StudentDetailViewModel(SavedStateHandle(mapOf("studentId" to "s1")), repository)

        assertEquals(UiState.Ready(student, false), vm.state)
    }

    // --- LibraryViewModel ---

    @Test
    fun `library view model defaults the display name to blank when absent`() = runTest {
        val repository = mockk<LibraryRepository>()
        coEvery { repository.getIssuesForStudent("s1") } returns Outcome.Success(emptyList())

        val vm = LibraryViewModel(SavedStateHandle(mapOf("studentId" to "s1")), repository)

        assertEquals("", vm.studentName)
    }

    @Test
    fun `library view model surfaces a failed fetch as an error state`() = runTest {
        val repository = mockk<LibraryRepository>()
        coEvery { repository.getIssuesForStudent("s1") } returns Outcome.Failure("No access")

        val vm = LibraryViewModel(SavedStateHandle(mapOf("studentId" to "s1", "name" to "Asha")), repository)

        assertEquals("Asha", vm.studentName)
        assertEquals(UiState.Error("No access"), vm.state)
    }

    @Test
    fun `library view model loads issues successfully`() = runTest {
        val repository = mockk<LibraryRepository>()
        val issue = BookIssue("i1", "b1", "Wonder", "s1", "Asha", "2026-07-01", "2026-07-15", null, null, BookIssueStatus.ISSUED)
        coEvery { repository.getIssuesForStudent("s1") } returns Outcome.Success(listOf(issue))

        val vm = LibraryViewModel(SavedStateHandle(mapOf("studentId" to "s1")), repository)

        assertEquals(UiState.Ready(listOf(issue), false), vm.state)
    }

    // --- TransportViewModel ---

    @Test
    fun `transport view model requires a studentId in the saved state`() {
        val repository = mockk<TransportRepository>()
        assertThrows(IllegalStateException::class.java) {
            TransportViewModel(SavedStateHandle(mapOf()), repository)
        }
    }

    @Test
    fun `transport view model loads the assignment and the first location poll`() = runTest {
        val repository = mockk<TransportRepository>()
        val assignment = StudentTransport("s1", "route1", "Route 1", "stop1", "Main Gate", 12.9, 77.6)
        coEvery { repository.getStudentTransport("s1") } returns Outcome.Success(assignment)
        coEvery { repository.getBusLocation("route1") } returns Outcome.Success(
            BusLocation(12.91, 77.61, "2026-07-16T09:00:00Z"),
        )

        val vm = TransportViewModel(SavedStateHandle(mapOf("studentId" to "s1")), repository)

        assertFalse(vm.loading)
        assertEquals(assignment, vm.assignment)
        assertNull(vm.error)
        assertEquals(12.91, vm.location?.latitude)

        // TransportViewModel.pollLocation() loops forever with delay() in production, cancelled
        // only by ViewModel.onCleared() — which nothing calls here. Cancel it explicitly so the
        // coroutine (and everything it closes over) doesn't leak for the rest of the test JVM's life.
        vm.viewModelScope.cancel()
    }

    @Test
    fun `transport view model surfaces a failed assignment lookup and never polls location`() = runTest {
        val repository = mockk<TransportRepository>()
        coEvery { repository.getStudentTransport("s1") } returns Outcome.Failure("Not assigned to a route")

        val vm = TransportViewModel(SavedStateHandle(mapOf("studentId" to "s1")), repository)

        assertFalse(vm.loading)
        assertNull(vm.assignment)
        assertEquals("Not assigned to a route", vm.error)
        assertNull(vm.location)
        io.mockk.coVerify(exactly = 0) { repository.getBusLocation(any()) }
    }
}
