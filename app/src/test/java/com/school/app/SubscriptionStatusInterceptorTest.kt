package com.school.app

import com.school.app.data.remote.SubscriptionStatusHolder
import com.school.app.data.remote.SubscriptionStatusInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubscriptionStatusInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var holder: SubscriptionStatusHolder
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        holder = SubscriptionStatusHolder()
        client = OkHttpClient.Builder()
            .addInterceptor(SubscriptionStatusInterceptor(holder))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun call(): okhttp3.Response =
        client.newCall(Request.Builder().url(server.url("/api/v1/students")).build()).execute()

    @Test
    fun `past due header on a successful response marks past due`() {
        server.enqueue(
            MockResponse()
                .setHeader("X-Subscription-Status", "PAST_DUE")
                .setBody("""[]"""),
        )

        call().use { }

        assertTrue(holder.isPastDue.value)
        assertFalse(holder.isSuspended.value)
    }

    @Test
    fun `plain successful response leaves the holder untouched`() {
        server.enqueue(MockResponse().setBody("""[]"""))

        call().use { }

        assertFalse(holder.isPastDue.value)
        assertFalse(holder.isSuspended.value)
    }

    @Test
    fun `403 with subscription suspended code marks suspended and keeps the body readable`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"status":403,"code":"SUBSCRIPTION_SUSPENDED","message":"Suspended"}"""),
        )

        call().use { response ->
            assertTrue(holder.isSuspended.value)
            // peekBody must not consume the body the caller sees.
            assertEquals(
                """{"status":403,"code":"SUBSCRIPTION_SUSPENDED","message":"Suspended"}""",
                response.body!!.string(),
            )
        }
    }

    @Test
    fun `403 with a different code does not mark suspended`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"status":403,"code":"FORBIDDEN","message":"No access"}"""),
        )

        call().use { }

        assertFalse(holder.isSuspended.value)
    }

    @Test
    fun `403 with a non-json body does not mark suspended`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        call().use { }

        assertFalse(holder.isSuspended.value)
    }
}
