package com.school.app.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.AuthRepository
import com.school.app.data.repository.BrandingRepository
import com.school.app.data.repository.SubscriptionRepository
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.domain.model.FeatureKey
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Null = no logo uploaded / not entitled; primaryColor null = using the app's default theme color. */
data class BrandingUi(val logo: ImageBitmap? = null, val primaryColor: Color? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncManager: AttendanceSyncManager,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val brandingRepository: BrandingRepository,
) : ViewModel() {

    val pendingAttendanceCount: StateFlow<Int> = syncManager.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Null = not fetched — GET /subscription is ADMIN-only, so for TEACHER/PARENT there is no
     * entitlement data to gate on; callers should treat null as permissive/allow-all, matching
     * the existing role behaviour. The backend's @RequiresEntitlement enforces every action
     * server-side regardless of what this shows.
     */
    private val _entitledFeatures = MutableStateFlow<Set<FeatureKey>?>(null)
    val entitledFeatures: StateFlow<Set<FeatureKey>?> = _entitledFeatures.asStateFlow()

    private val _branding = MutableStateFlow(BrandingUi())
    val branding: StateFlow<BrandingUi> = _branding.asStateFlow()

    init {
        viewModelScope.launch {
            val session = authRepository.session.first()
            if (session?.role == Role.ADMIN) {
                val result = subscriptionRepository.getCurrent()
                if (result is Outcome.Success) {
                    _entitledFeatures.value = result.data.entitlements
                        .filter { it.enabled }
                        .map { it.featureKey }
                        .toSet()
                }
            }
        }
        // Unlike GET /subscription, GET /branding is readable by every role — the whole app
        // themes itself from it.
        viewModelScope.launch {
            val brandingResult = brandingRepository.getCurrent()
            if (brandingResult !is Outcome.Success) return@launch

            val primaryColor = brandingResult.data.primaryColor?.let {
                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
            }
            val logo = if (brandingResult.data.hasLogo) {
                (brandingRepository.getLogoBitmap() as? Outcome.Success)?.data?.asImageBitmap()
            } else {
                null
            }
            _branding.value = BrandingUi(logo = logo, primaryColor = primaryColor)
        }
    }

    fun syncNow() = syncManager.syncNow()

    fun setLanguage(language: LanguageCode) {
        viewModelScope.launch { authRepository.updateLanguage(language) }
    }
}
