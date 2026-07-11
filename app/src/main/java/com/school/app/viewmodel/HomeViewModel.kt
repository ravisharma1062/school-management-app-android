package com.school.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.repository.AuthRepository
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.domain.model.LanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncManager: AttendanceSyncManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val pendingAttendanceCount: StateFlow<Int> = syncManager.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun syncNow() = syncManager.syncNow()

    fun setLanguage(language: LanguageCode) {
        viewModelScope.launch { authRepository.updateLanguage(language) }
    }
}
