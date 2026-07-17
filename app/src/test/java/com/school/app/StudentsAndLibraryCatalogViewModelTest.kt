package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.repository.LibraryRepository
import com.school.app.data.repository.StudentRepository
import com.school.app.domain.model.Book
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.Student
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.LibraryCatalogViewModel
import com.school.app.viewmodel.StudentsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudentsAndLibraryCatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- StudentsViewModel ---

    private val studentRepository = mockk<StudentRepository>()

    @Test
    fun `blank filters are sent as null rather than empty strings`() = runTest {
        // StudentsViewModel.init already calls refresh() once with the default (blank) filters,
        // before this test gets a chance to set classFilter — so both the default-state call and
        // the one this test triggers explicitly need a stub.
        coEvery { studentRepository.page(0, name = null, rollNo = null, studentClass = null) } returns
            Outcome.Success(PageResponse())
        coEvery { studentRepository.page(0, name = null, rollNo = null, studentClass = "5") } returns
            Outcome.Success(PageResponse())
        val vm = StudentsViewModel(studentRepository)
        vm.nameFilter = "  "
        vm.rollNoFilter = ""
        vm.classFilter = " 5 "

        vm.refresh()

        coVerify(exactly = 1) { studentRepository.page(0, name = null, rollNo = null, studentClass = "5") }
    }

    @Test
    fun `loadNext appends results and stops once the last page is reached`() = runTest {
        coEvery { studentRepository.page(0, name = null, rollNo = null, studentClass = null) } returns
            Outcome.Success(PageResponse(content = listOf(student("s1")), last = false))
        coEvery { studentRepository.page(1, name = null, rollNo = null, studentClass = null) } returns
            Outcome.Success(PageResponse(content = listOf(student("s2")), last = true))
        val vm = StudentsViewModel(studentRepository)

        vm.loadNext()

        assertEquals(listOf("s1", "s2"), vm.state.items.map { it.id })
        assertTrue(vm.state.endReached)

        vm.loadNext() // already at the end — must not fetch page 2
        coVerify(exactly = 0) { studentRepository.page(2, name = null, rollNo = null, studentClass = null) }
    }

    private fun student(id: String) = Student(id, "Student $id", "01", "5", "A", "2015-01-01")

    // --- LibraryCatalogViewModel ---

    private val libraryRepository = mockk<LibraryRepository>()

    @Test
    fun `an all-whitespace search term is sent as null`() = runTest {
        coEvery { libraryRepository.searchBooks(page = 0, search = null) } returns
            Outcome.Success(PageResponse(content = listOf(book("b1"))))
        val vm = LibraryCatalogViewModel(libraryRepository)
        vm.search = "   "

        vm.refresh()

        // LibraryCatalogViewModel.init already calls refresh() once with the default blank
        // search (also null once trimmed), so the explicit refresh() above is the second
        // matching call, not the first.
        coVerify(exactly = 2) { libraryRepository.searchBooks(page = 0, search = null) }
    }

    @Test
    fun `a failed search surfaces the error and keeps the loading flags clean`() = runTest {
        coEvery { libraryRepository.searchBooks(page = 0, search = null) } returns Outcome.Failure("Search failed")
        val vm = LibraryCatalogViewModel(libraryRepository)

        assertEquals("Search failed", vm.state.error)
        assertTrue(!vm.state.loading && !vm.state.loadingMore)
    }

    private fun book(id: String) = Book(id, "Wonder", "R.J. Palacio", null, false, 3, 2)
}
