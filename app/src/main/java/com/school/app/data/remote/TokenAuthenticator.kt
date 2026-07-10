package com.school.app.data.remote

import com.school.app.data.auth.TokenManager
import com.school.app.domain.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reacts to 401s by refreshing the access token (single-flight via the lock)
 * and replaying the failed request, mirroring the web client's interceptor.
 * A failed refresh clears the session, which sends the UI back to login.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val refreshApi: dagger.Lazy<RefreshApi>,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/refresh")) return null
        if (priorAttempts(response) >= 2) return null
        val refreshToken = tokenManager.refreshToken ?: return null

        synchronized(lock) {
            // Another request may have already refreshed while we waited on the lock.
            val current = tokenManager.accessToken
            val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (!current.isNullOrEmpty() && current != sent) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val refreshed = runBlocking {
                try {
                    refreshApi.get().refresh(RefreshRequest(refreshToken))
                        .also { tokenManager.saveTokens(it) }
                } catch (e: Exception) {
                    tokenManager.clear()
                    null
                }
            } ?: return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun priorAttempts(response: Response): Int {
        var count = 0
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
