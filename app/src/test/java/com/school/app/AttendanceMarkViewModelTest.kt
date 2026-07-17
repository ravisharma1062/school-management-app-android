package com.school.app

import androidx.lifecycle.SavedStateHandle
import com.school.app.data.Outcome
import com.school.app.data.repository.AttendanceRepository
import com.school.app.data.repository.StudentRepository
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.Student
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.AttendanceHistoryViewModel
import com.school.app.viewmodel.AttendanceMarkViewModel
import com.school.app.viewmodel.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttendanceMarkViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val studentRepository = mockk<StudentRepository>()
    private val attendanceRepository = mockk<AttendanceRepository>()
    private val syncManager = mockk<AttendanceSyncManager>()

    private fun viewModel(): AttendanceMarkViewModel {
        every { syncManager.pendingCount } returns MutableStateFlow(0)
        return AttendanceMarkViewModel(studentRepository, attendanceRepository, syncManager)
    }

    private fun student(id: String) = Student(id, "Student $id", "0$id", "5", "A", "2015-01-01")

    @Test
    fun `loadRoster rejects a blank class or section`() {
        val vm = viewModel()
        vm.studentClass = ""
        vm.section = "A"

        vm.loadRoster()

        assertEquals(AttendanceMarkViewModel.Roster.Error("Enter class and section"), vm.roster)
    }

    @Test
    fun `loadRoster reports when the class has no students`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(emptyList())

        vm.loadRoster()

        assertEquals(AttendanceMarkViewModel.Roster.Error("No students found in 5-A"), vm.roster)
    }

    @Test
    fun `loadRoster defaults every student to present and prefills existing marks`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        vm.date = java.time.LocalDate.of(2026, 7, 16)
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(listOf(student("s1"), student("s2")))
        coEvery { attendanceRepository.classOnDate("5", "A", "2026-07-16") } returns Outcome.Success(
            listOf(Attendance("a1", "s1", "2026-07-16", AttendanceStatus.ABSENT, "t1")),
        )

        vm.loadRoster()

        assertEquals(AttendanceStatus.ABSENT, vm.statuses["s1"])
        assertEquals(AttendanceStatus.PRESENT, vm.statuses["s2"])
        val ready = vm.roster as AttendanceMarkViewModel.Roster.Ready
        assertTrue(ready.alreadyMarked)
    }

    @Test
    fun `loadRoster with no existing marks is not flagged as already marked`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(listOf(student("s1")))
        coEvery { attendanceRepository.classOnDate("5", "A", any()) } returns Outcome.Success(emptyList())

        vm.loadRoster()

        val ready = vm.roster as AttendanceMarkViewModel.Roster.Ready
        assertTrue(!ready.alreadyMarked)
    }

    @Test
    fun `loadRoster surfaces a repository failure as a roster error`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Failure("Server error")

        vm.loadRoster()

        assertEquals(AttendanceMarkViewModel.Roster.Error("Server error"), vm.roster)
    }

    @Test
    fun `setStatus updates a single student without disturbing the others`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(listOf(student("s1"), student("s2")))
        coEvery { attendanceRepository.classOnDate("5", "A", any()) } returns Outcome.Success(emptyList())
        vm.loadRoster()

        vm.setStatus("s1", AttendanceStatus.LATE)

        assertEquals(AttendanceStatus.LATE, vm.statuses["s1"])
        assertEquals(AttendanceStatus.PRESENT, vm.statuses["s2"])
    }

    @Test
    fun `submit summarizes sent queued and failed counts into the result message`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(listOf(student("s1")))
        coEvery { attendanceRepository.classOnDate("5", "A", any()) } returns Outcome.Success(emptyList())
        vm.loadRoster()
        coEvery { attendanceRepository.submit(any(), any()) } returns AttendanceRepository.SubmitResult(
            sent = 1, queued = 2, failures = listOf("Bilal" to "Already marked"),
        )

        vm.submit()

        assertTrue(vm.resultMessage!!.contains("Saved 1 marks"))
        assertTrue(vm.resultMessage!!.contains("2 queued offline"))
        assertTrue(vm.resultMessage!!.contains("Bilal (Already marked)"))
    }

    @Test
    fun `submit with nothing to save reports that plainly`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "A"
        coEvery { studentRepository.allInClass("5", "A") } returns Outcome.Success(listOf(student("s1")))
        coEvery { attendanceRepository.classOnDate("5", "A", any()) } returns Outcome.Success(emptyList())
        vm.loadRoster()
        coEvery { attendanceRepository.submit(any(), any()) } returns
            AttendanceRepository.SubmitResult(sent = 0, queued = 0, failures = emptyList())

        vm.submit()

        assertEquals("Nothing to save", vm.resultMessage)
    }
}

class AttendanceHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val attendanceRepository = mockk<AttendanceRepository>()

    @Test
    fun `requires a studentId in the saved state`() {
        assertThrows(IllegalStateException::class.java) {
            AttendanceHistoryViewModel(SavedStateHandle(mapOf()), attendanceRepository)
        }
    }

    @Test
    fun `loads history for the student on init and defaults the name to blank`() = runTest {
        val history = listOf(Attendance("a1", "s1", "2026-07-16", AttendanceStatus.PRESENT, "t1"))
        coEvery { attendanceRepository.history("s1") } returns Outcome.Success(history)

        val vm = AttendanceHistoryViewModel(SavedStateHandle(mapOf("studentId" to "s1")), attendanceRepository)

        assertEquals("", vm.studentName)
        assertEquals(UiState.Ready(history, false), vm.state)
    }

    @Test
    fun `a fromCache result is reflected in the ui state`() = runTest {
        coEvery { attendanceRepository.history("s1") } returns
            Outcome.Success(emptyList(), fromCache = true)

        val vm = AttendanceHistoryViewModel(
            SavedStateHandle(mapOf("studentId" to "s1", "name" to "Asha")), attendanceRepository,
        )

        assertEquals("Asha", vm.studentName)
        assertEquals(UiState.Ready(emptyList<Attendance>(), true), vm.state)
    }
}
