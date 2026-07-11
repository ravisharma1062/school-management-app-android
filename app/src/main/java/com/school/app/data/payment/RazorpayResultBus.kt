package com.school.app.data.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface PaymentOutcome {
    data class Success(val paymentId: String, val orderId: String, val signature: String) : PaymentOutcome
    data class Error(val message: String) : PaymentOutcome
}

/** Bridges MainActivity's Razorpay SDK callback (onPaymentSuccess/onPaymentError) to whichever ViewModel started the checkout. */
object RazorpayResultBus {
    private val _events = MutableSharedFlow<PaymentOutcome>(extraBufferCapacity = 1)
    val events: SharedFlow<PaymentOutcome> = _events

    suspend fun emit(outcome: PaymentOutcome) {
        _events.emit(outcome)
    }
}
