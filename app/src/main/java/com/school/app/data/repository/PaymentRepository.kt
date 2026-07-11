package com.school.app.data.repository

import com.school.app.data.Outcome
import com.school.app.data.remote.ApiService
import com.school.app.data.safeApiCall
import com.school.app.domain.model.PaymentInitiateRequest
import com.school.app.domain.model.PaymentInitiateResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun initiate(feeId: String): Outcome<PaymentInitiateResponse> =
        safeApiCall { api.initiatePayment(PaymentInitiateRequest(feeId)) }
}
