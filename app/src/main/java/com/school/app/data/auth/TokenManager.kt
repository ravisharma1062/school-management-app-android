package com.school.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.school.app.domain.model.AuthResponse
import com.school.app.domain.model.Role
import com.school.app.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val role: Role,
    val userName: String?,
    val userEmail: String?,
)

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/**
 * JWT/session persistence backed by DataStore, with an in-memory copy of the
 * tokens so OkHttp interceptors can read them without suspending.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val ROLE = stringPreferencesKey("role")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
    }

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val access = prefs[Keys.ACCESS] ?: return@map null
        val refresh = prefs[Keys.REFRESH] ?: return@map null
        val role = prefs[Keys.ROLE]?.let { runCatching { Role.valueOf(it) }.getOrNull() }
            ?: return@map null
        Session(access, refresh, role, prefs[Keys.NAME], prefs[Keys.EMAIL])
    }

    /** Loads persisted tokens into memory; called once at app start. */
    suspend fun initialize() {
        val prefs = context.sessionDataStore.data.first()
        accessToken = prefs[Keys.ACCESS]
        refreshToken = prefs[Keys.REFRESH]
    }

    suspend fun saveTokens(auth: AuthResponse) {
        accessToken = auth.accessToken
        refreshToken = auth.refreshToken
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACCESS] = auth.accessToken
            prefs[Keys.REFRESH] = auth.refreshToken
            prefs[Keys.ROLE] = auth.role.name
        }
    }

    suspend fun saveUser(user: User) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.NAME] = user.name
            prefs[Keys.EMAIL] = user.email
        }
    }

    suspend fun clear() {
        accessToken = null
        refreshToken = null
        context.sessionDataStore.edit { it.clear() }
    }
}
