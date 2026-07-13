package com.school.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches every response for the two subscription-lifecycle signals JwtAuthFilter emits:
 * a 403 body with code SUBSCRIPTION_SUSPENDED (hard block), or an
 * X-Subscription-Status: PAST_DUE header on an otherwise-successful response (soft warning).
 */
@Singleton
class SubscriptionStatusInterceptor @Inject constructor(
    private val holder: SubscriptionStatusHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.header("X-Subscription-Status") == "PAST_DUE") {
            holder.markPastDue()
        }

        if (response.code == 403) {
            val code = runCatching {
                JSONObject(response.peekBody(2048).string()).optString("code")
            }.getOrNull()
            if (code == "SUBSCRIPTION_SUSPENDED") {
                holder.markSuspended()
            }
        }

        return response
    }
}
