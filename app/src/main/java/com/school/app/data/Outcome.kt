package com.school.app.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Result of a repository operation. [Success.fromCache] is true when the data
 * came from the local Room cache because the network was unavailable.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : Outcome<T>
    data class Failure(val message: String, val isNetwork: Boolean = false) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data), fromCache)
    is Outcome.Failure -> this
}

private val gson = Gson()

private data class BackendError(val message: String?, val error: String?)

/** Pulls the `message` out of the backend's standard ErrorResponse body. */
fun describeHttpError(e: HttpException): String {
    val body = try {
        e.response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    }
    val parsed = try {
        body?.let { gson.fromJson(it, BackendError::class.java) }
    } catch (_: Exception) {
        null
    }
    return parsed?.message ?: parsed?.error ?: "Request failed (${e.code()})"
}

suspend fun <T> safeApiCall(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: HttpException) {
    Outcome.Failure(describeHttpError(e))
} catch (e: IOException) {
    Outcome.Failure("You appear to be offline. Check your connection and try again.", isNetwork = true)
} catch (e: Exception) {
    Outcome.Failure(e.message ?: "Something went wrong")
}
