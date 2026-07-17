package com.school.app

import androidx.lifecycle.SavedStateHandle
import com.school.app.data.Outcome
import com.school.app.data.repository.TimetableRepository
import com.school.app.domain.model.TimetableEntry
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.TimetableViewModel
import com.school.app.viewmodel.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TimetableViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<TimetableRepository>()

    @Test
    fun `does not auto load without a class and section in the saved state`() {
        val vm = TimetableViewModel(SavedStateHandle(mapOf()), repository)

        assertNull(vm.state)
        coVerify(exactly = 0) { repository.forClass(any(), any()) }
    }

    @Test
    fun `auto loads when the nav args carry a class and section`() = runTest {
        val entry = TimetableEntry("tt1", "5", "A", "MONDAY", 1, "Maths", "t1")
        coEvery { repository.forClass("5", "A") } returns Outcome.Success(listOf(entry))

        val vm = TimetableViewModel(SavedStateHandle(mapOf("cls" to "5", "sec" to "A")), repository)

        assertEquals(UiState.Ready(listOf(entry), false), vm.state)
    }

    @Test
    fun `load with a blank class shows a validation error instead of calling the api`() {
        val vm = TimetableViewModel(SavedStateHandle(mapOf()), repository)
        vm.studentClass = "  "
        vm.section = "A"

        vm.load()

        assertEquals(UiState.Error("Enter class and section"), vm.state)
        coVerify(exactly = 0) { repository.forClass(any(), any()) }
    }
}
