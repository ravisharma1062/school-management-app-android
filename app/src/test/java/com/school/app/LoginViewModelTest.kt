package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.repository.AuthRepository
import com.school.app.domain.model.Role
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.LoginViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel = LoginViewModel(authRepository)

    @Test
    fun `blank email or password is rejected without calling the repository`() {
        viewModel.email = ""
        viewModel.password = "secret"

        viewModel.login()

        assertEquals("Enter your email and password", viewModel.error)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `successful login clears the error and flips submitting back off`() = runTest {
        viewModel.email = " admin@school.test "
        viewModel.password = "secret"
        coEvery { authRepository.login(" admin@school.test ", "secret") } returns Outcome.Success(Role.ADMIN)

        viewModel.login()
        advanceUntilIdle()

        assertNull(viewModel.error)
        assertFalse(viewModel.submitting)
    }

    @Test
    fun `failed login surfaces the backend message`() = runTest {
        viewModel.email = "admin@school.test"
        viewModel.password = "wrong"
        coEvery { authRepository.login(any(), any()) } returns Outcome.Failure("Invalid credentials")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.error)
        assertFalse(viewModel.submitting)
    }

    @Test
    fun `a second login call while the first is still in flight is ignored`() = runTest {
        viewModel.email = "admin@school.test"
        viewModel.password = "secret"
        // A pending deferred keeps the first call suspended, mirroring an in-flight network
        // request, so the guard (checked synchronously at the top of login()) has something
        // real to guard against — with Main.immediate the launch body runs eagerly up to here.
        val gate = CompletableDeferred<Outcome<Role>>()
        coEvery { authRepository.login(any(), any()) } coAnswers { gate.await() }

        viewModel.login()
        assertEquals(true, viewModel.submitting)
        viewModel.login() // should be dropped by the `if (submitting) return` guard

        gate.complete(Outcome.Success(Role.TEACHER))
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.login(any(), any()) }
        assertFalse(viewModel.submitting)
    }
}
