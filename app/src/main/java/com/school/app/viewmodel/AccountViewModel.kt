package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.repository.AuthRepository
import com.school.app.data.repository.SubscriptionRepository
import com.school.app.domain.model.SubscriptionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<SubscriptionDto>>(UiState.Loading)
        private set

    /** MT-6e — read-only on Android; reassignment is a web-only action (Users page). */
    val isBillingOwner: StateFlow<Boolean> = authRepository.session
        .map { it?.isBillingOwner ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = subscriptionRepository.getCurrent().toUiState()
        }
    }
}
