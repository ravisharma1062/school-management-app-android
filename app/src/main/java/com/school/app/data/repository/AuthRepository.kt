package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.auth.Session
import com.school.app.data.auth.TokenManager
import com.school.app.data.map
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.LoginRequest
import com.school.app.domain.model.Role
import com.school.app.domain.model.UserLanguageUpdateRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager,
) {
    /** Emits null when logged out; the UI reacts by showing the login screen. */
    val session: Flow<Session?> = tokenManager.session

    suspend fun login(email: String, password: String): Outcome<Role> {
        val result = safeApiCall { api.login(LoginRequest(email.trim(), password)) }
        return when (result) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                tokenManager.saveTokens(result.data)
                // Best-effort profile fetch for the greeting; login still counts if it fails.
                safeApiCall { api.me() }.let { me ->
                    if (me is Outcome.Success) tokenManager.saveUser(me.data)
                }
                Outcome.Success(result.data.role)
            }
        }
    }

    suspend fun logout() {
        tokenManager.clear()
    }

    suspend fun updateLanguage(language: LanguageCode): Outcome<LanguageCode> {
        val result = safeApiCall { api.updateLanguage(UserLanguageUpdateRequest(language)) }
        if (result is Outcome.Success) {
            tokenManager.saveLanguage(result.data.preferredLanguage)
        }
        return result.map { it.preferredLanguage }
    }
}
