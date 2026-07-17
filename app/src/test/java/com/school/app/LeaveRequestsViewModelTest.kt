package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.repository.LeaveRequestRepository
import com.school.app.domain.model.LeaveRequest
import com.school.app.domain.model.LeaveRequestCreateRequest
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.LeaveType
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.Role
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.LeaveRequestsViewModel
import com.school.app.viewmodel.canReviewLeaveRequests
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class LeaveRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<LeaveRequestRepository>()

    private fun freshViewModel(): LeaveRequestsViewModel {
        coEvery { repository.page(0, status = null) } returns Outcome.Success(PageResponse(last = true))
        return LeaveRequestsViewModel(repository)
    }

    @Test
    fun `only ADMIN can review leave requests`() {
        assertTrue(canReviewLeaveRequests(Role.ADMIN))
        assertFalse(canReviewLeaveRequests(Role.TEACHER))
        assertFalse(canReviewLeaveRequests(Role.PARENT))
    }

    @Test
    fun `submit rejects an end date before the start date without calling the repository`() = runTest {
        val vm = freshViewModel()
        vm.fromDate = LocalDate.of(2026, 7, 20)
        vm.toDate = LocalDate.of(2026, 7, 18)

        vm.submit()

        assertEquals("End date must not be before start date", vm.submitError)
        assertFalse(vm.submitSuccess)
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `submit trims a blank reason down to null`() = runTest {
        val vm = freshViewModel()
        vm.fromDate = LocalDate.of(2026, 7, 20)
        vm.toDate = LocalDate.of(2026, 7, 20)
        vm.reason = "   "
        vm.type = LeaveType.SICK
        coEvery {
            repository.create(LeaveRequestCreateRequest(LeaveType.SICK, "2026-07-20", "2026-07-20", null))
        } returns Outcome.Success(
            LeaveRequest("lr1", "u1", LeaveType.SICK, "2026-07-20", "2026-07-20", null, LeaveStatus.PENDING, null, "2026-07-16T00:00:00Z"),
        )

        vm.submit()

        assertTrue(vm.submitSuccess)
        assertEquals("", vm.reason)
        assertFalse(vm.submitting)
    }

    @Test
    fun `a failed submit surfaces the message and does not clear the form`() = runTest {
        val vm = freshViewModel()
        vm.fromDate = LocalDate.of(2026, 7, 20)
        vm.toDate = LocalDate.of(2026, 7, 20)
        vm.reason = "Fever"
        coEvery { repository.create(any()) } returns Outcome.Failure("Overlapping leave request")

        vm.submit()

        assertEquals("Overlapping leave request", vm.submitError)
        assertFalse(vm.submitSuccess)
        assertEquals("Fever", vm.reason)
    }

    @Test
    fun `review guards against concurrent reviews and refreshes afterwards`() = runTest {
        val vm = freshViewModel()
        coEvery { repository.review("lr1", LeaveStatus.APPROVED) } returns Outcome.Success(
            LeaveRequest("lr1", "u1", LeaveType.SICK, "2026-07-20", "2026-07-20", null, LeaveStatus.APPROVED, "admin1", "2026-07-16T00:00:00Z"),
        )

        vm.review("lr1", LeaveStatus.APPROVED)

        coVerify(exactly = 1) { repository.review("lr1", LeaveStatus.APPROVED) }
        // refresh() re-queries page 0, so it's called a second time (once at init, once after review).
        coVerify(exactly = 2) { repository.page(0, status = null) }
    }

    @Test
    fun `loadNext does nothing once the list is exhausted`() = runTest {
        val vm = freshViewModel() // page(0) already returned last = true

        vm.loadNext()

        coVerify(exactly = 0) { repository.page(1, status = null) }
    }
}
