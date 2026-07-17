package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.auth.Session
import com.school.app.data.repository.AuthRepository
import com.school.app.data.repository.SubscriptionRepository
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.PlanCode
import com.school.app.domain.model.Role
import com.school.app.domain.model.SchoolStatus
import com.school.app.domain.model.SubscriptionDto
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.AccountViewModel
import com.school.app.viewmodel.UiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val authRepository = mockk<AuthRepository>()

    private fun session(billingOwner: Boolean) = Session(
        accessToken = "a", refreshToken = "r", role = Role.ADMIN, userId = "u1",
        userName = "Admin", userEmail = "a@x.test", preferredLanguage = LanguageCode.EN,
        isBillingOwner = billingOwner,
    )

    @Test
    fun `loads the subscription into state`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session(false))
        val dto = SubscriptionDto(PlanCode.STANDARD, "Standard", SchoolStatus.ACTIVE)
        io.mockk.coEvery { subscriptionRepository.getCurrent() } returns Outcome.Success(dto)

        val vm = AccountViewModel(subscriptionRepository, authRepository)

        assertEquals(UiState.Ready(dto, false), vm.state)
    }

    @Test
    fun `a failed subscription fetch surfaces the error`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session(false))
        io.mockk.coEvery { subscriptionRepository.getCurrent() } returns Outcome.Failure("Not entitled")

        val vm = AccountViewModel(subscriptionRepository, authRepository)

        assertEquals(UiState.Error("Not entitled"), vm.state)
    }

    @Test
    fun `isBillingOwner mirrors the session flag`() = runTest {
        every { authRepository.session } returns MutableStateFlow(session(true))
        io.mockk.coEvery { subscriptionRepository.getCurrent() } returns
            Outcome.Success(SubscriptionDto(PlanCode.BASIC, "Basic", SchoolStatus.TRIAL))

        val vm = AccountViewModel(subscriptionRepository, authRepository)

        // isBillingOwner is stateIn(WhileSubscribed) — .value alone won't trigger upstream
        // collection, so subscribe via first() to force the mapped session through.
        assertTrue(vm.isBillingOwner.first())
    }

    @Test
    fun `isBillingOwner defaults to false when there is no session`() = runTest {
        every { authRepository.session } returns MutableStateFlow(null)
        io.mockk.coEvery { subscriptionRepository.getCurrent() } returns Outcome.Failure("no session")

        val vm = AccountViewModel(subscriptionRepository, authRepository)

        assertEquals(false, vm.isBillingOwner.first())
    }
}
