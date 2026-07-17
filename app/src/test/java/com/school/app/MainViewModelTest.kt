package com.school.app

import com.school.app.data.auth.Session
import com.school.app.data.remote.SubscriptionStatusHolder
import com.school.app.data.repository.AuthRepository
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.Role
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.MainViewModel
import com.school.app.viewmodel.SessionUi
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val holder = SubscriptionStatusHolder()

    private val session = Session(
        accessToken = "a", refreshToken = "r", role = Role.PARENT, userId = "u1",
        userName = "Parent", userEmail = "p@x.test", preferredLanguage = LanguageCode.EN,
    )

    @Test
    fun `logged out session maps to LoggedOut`() = runTest {
        every { authRepository.session } returns MutableStateFlow(null)

        val vm = MainViewModel(authRepository, holder)

        assertEquals(SessionUi.LoggedOut, vm.sessionState.value)
    }

    @Test
    fun `an active session maps to LoggedIn carrying that session`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session)

        val vm = MainViewModel(authRepository, holder)

        assertEquals(SessionUi.LoggedIn(session), vm.sessionState.value)
    }

    @Test
    fun `subscription status flows through unchanged from the holder`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session)
        holder.markPastDue()

        val vm = MainViewModel(authRepository, holder)

        assertTrue(vm.isPastDue.value)
        assertEquals(false, vm.isSuspended.value)
    }

    @Test
    fun `dismissing the past due banner clears it on the shared holder`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session)
        holder.markPastDue()
        val vm = MainViewModel(authRepository, holder)

        vm.dismissPastDueBanner()

        assertEquals(false, holder.isPastDue.value)
    }

    @Test
    fun `logout delegates to the repository`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session)
        coJustRun { authRepository.logout() }
        val vm = MainViewModel(authRepository, holder)

        vm.logout()

        coVerify(exactly = 1) { authRepository.logout() }
    }
}
