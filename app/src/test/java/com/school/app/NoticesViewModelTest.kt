package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.repository.NoticeRepository
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.TargetRole
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.NoticesViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoticesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<NoticeRepository>()

    private fun notice(id: String) =
        Notice(id, "Title $id", null, TargetRole.ALL, "admin1", "2026-07-16T00:00:00Z")

    @Test
    fun `initial load populates the first page`() = runTest {
        coEvery { repository.page(0, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n1"), notice("n2")), last = false),
        )

        val vm = NoticesViewModel(repository)

        assertEquals(listOf("n1", "n2"), vm.state.items.map { it.id })
        assertFalse(vm.state.loading)
        assertFalse(vm.state.endReached)
    }

    @Test
    fun `loadNext appends to the existing items and advances the page`() = runTest {
        coEvery { repository.page(0, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n1")), last = false),
        )
        coEvery { repository.page(1, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n2")), last = true),
        )
        val vm = NoticesViewModel(repository)

        vm.loadNext()

        assertEquals(listOf("n1", "n2"), vm.state.items.map { it.id })
        assertTrue(vm.state.endReached)
        assertEquals(1, vm.state.page)
    }

    @Test
    fun `loadNext is a no op once the end has been reached`() = runTest {
        coEvery { repository.page(0, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n1")), last = true),
        )
        val vm = NoticesViewModel(repository)

        vm.loadNext()

        io.mockk.coVerify(exactly = 1) { repository.page(any(), any()) }
    }

    @Test
    fun `a failed refresh surfaces the error and keeps loading false`() = runTest {
        coEvery { repository.page(0, any()) } returns Outcome.Failure("Server error")

        val vm = NoticesViewModel(repository)

        assertEquals("Server error", vm.state.error)
        assertFalse(vm.state.loading)
        assertTrue(vm.state.items.isEmpty())
    }

    @Test
    fun `refresh resets back to page zero and clears stale items`() = runTest {
        coEvery { repository.page(0, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n1")), last = true),
        )
        val vm = NoticesViewModel(repository)
        coEvery { repository.page(0, any()) } returns Outcome.Success(
            PageResponse(content = listOf(notice("n9")), last = false),
        )

        vm.refresh()

        assertEquals(listOf("n9"), vm.state.items.map { it.id })
        assertEquals(0, vm.state.page)
    }
}
