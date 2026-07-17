package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.describeHttpError
import com.school.app.data.map
import com.school.app.data.safeApiCall
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Edge cases around [Outcome.map], [describeHttpError] and [safeApiCall] beyond OutcomeTest. */
class OutcomeMapAndErrorTest {

    private fun httpException(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `map transforms success data and preserves fromCache`() {
        val cached: Outcome<Int> = Outcome.Success(21, fromCache = true)
        val mapped = cached.map { it * 2 }
        assertEquals(Outcome.Success(42, fromCache = true), mapped)
    }

    @Test
    fun `map passes a failure through untouched`() {
        val failure: Outcome<Int> = Outcome.Failure("boom", isNetwork = true)
        val mapped: Outcome<String> = failure.map { it.toString() }
        assertSame(failure, mapped)
    }

    @Test
    fun `describeHttpError falls back to the error field when message is absent`() {
        val e = httpException(403, """{"status":403,"error":"Forbidden"}""")
        assertEquals("Forbidden", describeHttpError(e))
    }

    @Test
    fun `describeHttpError prefers message over error`() {
        val e = httpException(400, """{"error":"Bad Request","message":"Roll number already exists"}""")
        assertEquals("Roll number already exists", describeHttpError(e))
    }

    @Test
    fun `describeHttpError with malformed json falls back to the status code`() {
        val e = httpException(502, "<html>Bad Gateway</html>")
        assertEquals("Request failed (502)", describeHttpError(e))
    }

    @Test
    fun `describeHttpError with empty json object falls back to the status code`() {
        val e = httpException(500, "{}")
        assertEquals("Request failed (500)", describeHttpError(e))
    }

    @Test
    fun `unexpected exception surfaces its message`() = runTest {
        val result = safeApiCall<Unit> { throw IllegalStateException("weird state") }
        val failure = result as Outcome.Failure
        assertEquals("weird state", failure.message)
        assertTrue(!failure.isNetwork)
    }

    @Test
    fun `unexpected exception without message gets a generic one`() = runTest {
        val result = safeApiCall<Unit> { throw RuntimeException() }
        assertEquals("Something went wrong", (result as Outcome.Failure).message)
    }

    @Test
    fun `success from safeApiCall is never marked fromCache`() = runTest {
        val result = safeApiCall { "data" } as Outcome.Success
        assertTrue(!result.fromCache)
    }
}
