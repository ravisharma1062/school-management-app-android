package com.school.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.school.app.data.payment.PaymentOutcome
import com.school.app.data.payment.RazorpayResultBus
import com.school.app.ui.navigation.AppRoot
import com.school.app.ui.theme.SchoolAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolAppTheme {
                AppRoot()
            }
        }
    }

    // Fired by the Razorpay Checkout SDK once the payment sheet it launched resolves.
    // The actual Fee/Payment record update happens server-side via webhook, so this is
    // only a "checkout completed" signal for the UI, not proof of a reconciled payment.
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            RazorpayResultBus.emit(
                PaymentOutcome.Success(
                    paymentId = razorpayPaymentId.orEmpty(),
                    orderId = paymentData?.orderId.orEmpty(),
                    signature = paymentData?.signature.orEmpty(),
                ),
            )
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            RazorpayResultBus.emit(PaymentOutcome.Error(description ?: "Payment was not completed."))
        }
    }
}
