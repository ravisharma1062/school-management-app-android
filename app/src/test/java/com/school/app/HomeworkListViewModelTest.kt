package com.school.app

import androidx.lifecycle.SavedStateHandle
import com.school.app.data.Outcome
import com.school.app.data.repository.HomeworkRepository
import com.school.app.data.repository.HomeworkSubmissionRepository
import com.school.app.data.repository.StudentRepository
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkSubmission
import com.school.app.domain.model.HomeworkSubmissionStatus
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.Student
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.HomeworkListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeworkListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val homeworkRepository = mockk<HomeworkRepository>()
    private val submissionRepository = mockk<HomeworkSubmissionRepository>()
    private val studentRepository = mockk<StudentRepository>()

    private fun homework(id: String) =
        Homework(id, "5", "A", "Maths", "Fractions", null, "2026-07-20", "t1", "2026-07-16T00:00:00Z")

    private fun viewModel(cls: String = "", sec: String = "") = HomeworkListViewModel(
        SavedStateHandle(mapOf("cls" to cls, "sec" to sec)),
        homeworkRepository, submissionRepository, studentRepository,
    )

    @Test
    fun `does not auto load when the nav args have no class or section`() {
        val vm = viewModel()

        assertTrue(vm.state.items.isEmpty())
        coVerify(exactly = 0) { homeworkRepository.page(any(), any(), any(), any()) }
    }

    @Test
    fun `auto loads when the nav args carry a class and section`() = runTest {
        coEvery { homeworkRepository.page("5", "A", 0, 20) } returns Outcome.Success(
            PageResponse(content = listOf(homework("h1")), last = true),
        )

        val vm = viewModel(cls = "5", sec = "A")

        assertEquals(listOf("h1"), vm.state.items.map { it.id })
    }

    @Test
    fun `refresh with a blank class or section shows a validation error instead of calling the api`() = runTest {
        val vm = viewModel()
        vm.studentClass = "5"
        vm.section = "  "

        vm.refresh()

        assertEquals("Enter class and section", vm.state.error)
        coVerify(exactly = 0) { homeworkRepository.page(any(), any(), any(), any()) }
    }

    @Test
    fun `loadMyChildrenForCurrentClass filters to the loaded class and section case insensitively`() = runTest {
        coEvery { homeworkRepository.page("5", "A", 0, 20) } returns Outcome.Success(
            PageResponse(content = listOf(homework("h1")), last = true),
        )
        val vm = viewModel(cls = "5", sec = "A")
        val inClass = Student("s1", "Asha", "01", "5", "a", "2015-01-01") // lower-case section
        val otherClass = Student("s2", "Bilal", "02", "6", "A", "2015-01-01")
        coEvery { studentRepository.myChildren() } returns Outcome.Success(listOf(inClass, otherClass))
        coEvery { submissionRepository.byStudent("s1") } returns Outcome.Success(emptyList())

        vm.loadMyChildrenForCurrentClass()

        assertEquals(listOf("s1"), vm.myChildrenInLoadedClass.map { it.id })
        coVerify(exactly = 1) { submissionRepository.byStudent("s1") }
        coVerify(exactly = 0) { submissionRepository.byStudent("s2") }
    }

    @Test
    fun `submit guards against starting a second upload while one is in flight`() = runTest {
        coEvery { homeworkRepository.page("5", "A", 0, 20) } returns Outcome.Success(
            PageResponse(content = listOf(homework("h1")), last = true),
        )
        val vm = viewModel(cls = "5", sec = "A")
        val gate = kotlinx.coroutines.CompletableDeferred<Outcome<HomeworkSubmission>>()
        val uri = mockk<android.net.Uri>(relaxed = true)
        coEvery { submissionRepository.submit("h1", "s1", uri) } coAnswers { gate.await() }
        // A successful submit() triggers loadSubmissionsForStudent("s1") as a follow-up call.
        coEvery { submissionRepository.byStudent("s1") } returns Outcome.Success(emptyList())

        vm.submit("h1", "s1", uri)
        assertEquals("s1", vm.submittingStudentId)
        vm.submit("h1", "s2", uri)

        gate.complete(
            Outcome.Success(
                HomeworkSubmission("sub1", "h1", "s1", "f.pdf", "application/pdf", HomeworkSubmissionStatus.SUBMITTED, null, null, "2026-07-16T00:00:00Z"),
            ),
        )

        coVerify(exactly = 1) { submissionRepository.submit(any(), any(), any()) }
    }

    @Test
    fun `toggleExpanded loads submissions once then collapses without refetching`() = runTest {
        coEvery { homeworkRepository.page("5", "A", 0, 20) } returns Outcome.Success(
            PageResponse(content = listOf(homework("h1")), last = true),
        )
        val vm = viewModel(cls = "5", sec = "A")
        coEvery { submissionRepository.byHomework("h1") } returns Outcome.Success(emptyList())

        vm.toggleExpanded("h1")
        assertTrue(vm.expandedHomeworkIds.containsKey("h1"))

        vm.toggleExpanded("h1")
        assertTrue(!vm.expandedHomeworkIds.containsKey("h1"))
        coVerify(exactly = 1) { submissionRepository.byHomework("h1") }
    }
}
