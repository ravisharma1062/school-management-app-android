package com.school.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.auth.Session
import com.school.app.data.remote.SubscriptionStatusHolder
import com.school.app.data.repository.AuthRepository
import com.school.app.fcm.PushTopics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionUi {
    data object Loading : SessionUi
    data object LoggedOut : SessionUi
    data class LoggedIn(val session: Session) : SessionUi
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionStatusHolder: SubscriptionStatusHolder,
) : ViewModel() {

    val sessionState: StateFlow<SessionUi> = authRepository.session
        .map<Session?, SessionUi> { session ->
            if (session == null) SessionUi.LoggedOut else SessionUi.LoggedIn(session)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionUi.Loading)

    val isSuspended: StateFlow<Boolean> = subscriptionStatusHolder.isSuspended
    val isPastDue: StateFlow<Boolean> = subscriptionStatusHolder.isPastDue

    fun dismissPastDueBanner() = subscriptionStatusHolder.dismissPastDue()

    fun logout() {
        viewModelScope.launch {
            PushTopics.unsubscribeAll()
            authRepository.logout()
        }
    }
}
