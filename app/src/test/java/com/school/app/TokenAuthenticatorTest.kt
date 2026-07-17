package com.school.app

import com.school.app.data.auth.TokenManager
import com.school.app.data.remote.AuthInterceptor
import com.school.app.data.remote.RefreshApi
import com.school.app.data.remote.TokenAuthenticator
import com.school.app.domain.model.AuthResponse
import com.school.app.domain.model.RefreshRequest
import com.school.app.domain.model.Role
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenAuthenticatorTest {

    private val tokenManager = mockk<TokenManager>()
    private val refreshApi = mockk<RefreshApi>()
    private val authenticator = TokenAuthenticator(tokenManager, dagger.Lazy { refreshApi })

    private fun request(path: String, token: String? = null): Request =
        Request.Builder()
            .url("http://localhost$path")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

    private fun unauthorized(request: Request, priorCount: Int = 0): Response {
        var response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
        repeat(priorCount) {
            response = response.newBuilder().priorResponse(response).build()
        }
        return response
    }

    @Test
    fun `never refreshes for the login endpoint`() {
        assertNull(authenticator.authenticate(null, unauthorized(request("/api/v1/auth/login"))))
    }

    @Test
    fun `never refreshes for the refresh endpoint itself`() {
        assertNull(authenticator.authenticate(null, unauthorized(request("/api/v1/auth/refresh"))))
    }

    @Test
    fun `gives up after two prior attempts`() {
        every { tokenManager.refreshToken } returns "refresh"
        val response = unauthorized(request("/api/v1/students", token = "old"), priorCount = 2)
        assertNull(authenticator.authenticate(null, response))
    }

    @Test
    fun `gives up when there is no refresh token`() {
        every { tokenManager.refreshToken } returns null
        assertNull(authenticator.authenticate(null, unauthorized(request("/api/v1/students", token = "old"))))
    }

    @Test
    fun `reuses a token already refreshed by another request without calling the api`() {
        every { tokenManager.refreshToken } returns "refresh"
        every { tokenManager.accessToken } returns "already-refreshed"

        val result = authenticator.authenticate(
            null,
            unauthorized(request("/api/v1/students", token = "old")),
        )

        assertEquals("Bearer already-refreshed", result!!.header("Authorization"))
        coVerify(exactly = 0) { refreshApi.refresh(any()) }
    }

    @Test
    fun `refreshes and replays the request with the new token`() {
        every { tokenManager.refreshToken } returns "refresh"
        every { tokenManager.accessToken } returns "old"
        coEvery { refreshApi.refresh(RefreshRequest("refresh")) } returns
            AuthResponse("new-token", "new-refresh", Role.TEACHER)
        coJustRun { tokenManager.saveTokens(any()) }

        val result = authenticator.authenticate(
            null,
            unauthorized(request("/api/v1/students", token = "old")),
        )

        assertEquals("Bearer new-token", result!!.header("Authorization"))
        coVerify(exactly = 1) { tokenManager.saveTokens(AuthResponse("new-token", "new-refresh", Role.TEACHER)) }
    }

    @Test
    fun `failed refresh clears the session and gives up`() {
        every { tokenManager.refreshToken } returns "refresh"
        every { tokenManager.accessToken } returns "old"
        coEvery { refreshApi.refresh(any()) } throws RuntimeException("401 on refresh")
        coJustRun { tokenManager.clear() }

        val result = authenticator.authenticate(
            null,
            unauthorized(request("/api/v1/students", token = "old")),
        )

        assertNull(result)
        coVerify(exactly = 1) { tokenManager.clear() }
    }

    /** End-to-end through OkHttp: a 401 is transparently retried with the refreshed token. */
    @Test
    fun `okhttp replays a 401 with the refreshed token`() {
        val server = MockWebServer()
        server.start()
        try {
            var currentToken = "stale"
            every { tokenManager.accessToken } answers { currentToken }
            every { tokenManager.refreshToken } returns "refresh"
            coEvery { refreshApi.refresh(any()) } returns AuthResponse("fresh", "refresh2", Role.ADMIN)
            coEvery { tokenManager.saveTokens(any()) } answers {
                currentToken = firstArg<AuthResponse>().accessToken
            }

            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"expired"}"""))
            server.enqueue(MockResponse().setBody("""{"ok":true}"""))

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .authenticator(authenticator)
                .build()
            val response = client.newCall(
                Request.Builder().url(server.url("/api/v1/students")).build(),
            ).execute()

            response.use { assertEquals(200, it.code) }
            assertEquals("Bearer stale", server.takeRequest().getHeader("Authorization"))
            assertEquals("Bearer fresh", server.takeRequest().getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}
