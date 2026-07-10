package com.school.app

import com.school.app.data.Outcome
import com.school.app.data.safeApiCall
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OutcomeTest {

    @Test
    fun `success wraps the value`() = runTest {
        val result = safeApiCall { 42 }
        assertEquals(Outcome.Success(42), result)
    }

    @Test
    fun `http error surfaces the backend error message`() = runTest {
        val body = """{"status":404,"error":"Not Found","message":"Student with id x not found"}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<Unit> { throw HttpException(Response.error<Any>(404, body)) }
        val failure = result as Outcome.Failure
        assertEquals("Student with id x not found", failure.message)
        assertTrue(!failure.isNetwork)
    }

    @Test
    fun `http error without body falls back to status code`() = runTest {
        val body = "".toResponseBody("application/json".toMediaType())
        val result = safeApiCall<Unit> { throw HttpException(Response.error<Any>(500, body)) }
        val failure = result as Outcome.Failure
        assertEquals("Request failed (500)", failure.message)
    }

    @Test
    fun `io exception is flagged as a network failure`() = runTest {
        val result = safeApiCall<Unit> { throw IOException("timeout") }
        val failure = result as Outcome.Failure
        assertTrue(failure.isNetwork)
    }
}
