package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.AuthRepository
import com.school.app.fcm.PushTopics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun login() {
        if (submitting) return
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your email and password"
            return
        }
        viewModelScope.launch {
            submitting = true
            error = null
            when (val result = authRepository.login(email, password)) {
                is Outcome.Success -> PushTopics.subscribeFor(result.data)
                // Session flow flips to logged-in on success; nothing to navigate here.
                is Outcome.Failure -> error = result.message
            }
            submitting = false
        }
    }
}
