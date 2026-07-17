package com.school.app

import com.school.app.data.auth.TokenManager
import com.school.app.data.remote.AuthInterceptor
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private val tokenManager = mockk<TokenManager>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun execute() {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
        server.enqueue(MockResponse().setBody("{}"))
        client.newCall(Request.Builder().url(server.url("/api/v1/notices")).build())
            .execute()
            .use { }
    }

    @Test
    fun `attaches a bearer token when one is available`() {
        every { tokenManager.accessToken } returns "abc123"

        execute()

        assertEquals("Bearer abc123", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `sends the request untouched when logged out`() {
        every { tokenManager.accessToken } returns null

        execute()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
