package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.auth.Session
import com.school.app.data.remote.SubscriptionStatusHolder
import com.school.app.data.repository.AuthRepository
import com.school.app.data.repository.BrandingRepository
import com.school.app.data.repository.SubscriptionRepository
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.domain.model.BrandingDto
import com.school.app.domain.model.EntitlementDto
import com.school.app.domain.model.FeatureKey
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.PlanCode
import com.school.app.domain.model.Role
import com.school.app.domain.model.SchoolStatus
import com.school.app.domain.model.SubscriptionDto
import com.school.app.util.MainDispatcherRule
import com.school.app.viewmodel.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.OffsetDateTime

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val syncManager = mockk<AttendanceSyncManager>()
    private val authRepository = mockk<AuthRepository>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val brandingRepository = mockk<BrandingRepository>()

    private val noLogoBranding = Outcome.Success(BrandingDto(hasLogo = false, primaryColor = null))

    private fun session(role: Role) = Session(
        accessToken = "a", refreshToken = "r", role = role, userId = "u1",
        userName = "Name", userEmail = "n@x.test", preferredLanguage = LanguageCode.EN,
    )

    private fun viewModel(): HomeViewModel {
        every { syncManager.pendingCount } returns MutableStateFlow(0)
        coEvery { brandingRepository.getCurrent() } returns noLogoBranding
        return HomeViewModel(syncManager, authRepository, subscriptionRepository, brandingRepository)
    }

    @Test
    fun `non admin never queries the subscription`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.TEACHER))

        val vm = viewModel()

        assertNull(vm.trialDaysLeft.value)
        assertNull(vm.entitledFeatures.value)
        coVerify(exactly = 0) { subscriptionRepository.getCurrent() }
    }

    @Test
    fun `admin on trial gets days left computed from trialEndsAt`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.ADMIN))
        val trialEndsAt = OffsetDateTime.now().plusDays(5)
        coEvery { subscriptionRepository.getCurrent() } returns Outcome.Success(
            SubscriptionDto(
                planCode = PlanCode.BASIC,
                planName = "Basic",
                status = SchoolStatus.TRIAL,
                trialEndsAt = trialEndsAt.toString(),
                entitlements = listOf(
                    EntitlementDto(FeatureKey.MESSAGING, enabled = true),
                    EntitlementDto(FeatureKey.LIBRARY, enabled = false),
                ),
            ),
        )

        val vm = viewModel()

        assertEquals(setOf(FeatureKey.MESSAGING), vm.entitledFeatures.value)
        // Allow a 1-day tolerance for wall-clock drift between here and inside the ViewModel.
        assertTrue(vm.trialDaysLeft.value in 4..5)
    }

    @Test
    fun `admin not on trial has no trial days shown`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.ADMIN))
        coEvery { subscriptionRepository.getCurrent() } returns Outcome.Success(
            SubscriptionDto(
                planCode = PlanCode.PREMIUM,
                planName = "Premium",
                status = SchoolStatus.ACTIVE,
                trialEndsAt = OffsetDateTime.now().plusDays(5).toString(),
            ),
        )

        val vm = viewModel()

        assertNull(vm.trialDaysLeft.value)
    }

    @Test
    fun `trial with an unparseable trialEndsAt leaves days left null instead of crashing`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.ADMIN))
        coEvery { subscriptionRepository.getCurrent() } returns Outcome.Success(
            SubscriptionDto(
                planCode = PlanCode.BASIC,
                planName = "Basic",
                status = SchoolStatus.TRIAL,
                trialEndsAt = "not-a-real-timestamp",
            ),
        )

        val vm = viewModel()

        assertNull(vm.trialDaysLeft.value)
    }

    @Test
    fun `trial that already ended clamps to zero days instead of going negative`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.ADMIN))
        coEvery { subscriptionRepository.getCurrent() } returns Outcome.Success(
            SubscriptionDto(
                planCode = PlanCode.BASIC,
                planName = "Basic",
                status = SchoolStatus.TRIAL,
                trialEndsAt = OffsetDateTime.now().minusDays(3).toString(),
            ),
        )

        val vm = viewModel()

        assertEquals(0, vm.trialDaysLeft.value)
    }

    @Test
    fun `subscription fetch failure leaves entitlements and trial state at their defaults`() = runTest {
        every { authRepository.session } returns flowOf(session(Role.ADMIN))
        coEvery { subscriptionRepository.getCurrent() } returns Outcome.Failure("down")

        val vm = viewModel()

        assertNull(vm.entitledFeatures.value)
        assertNull(vm.trialDaysLeft.value)
    }
}
