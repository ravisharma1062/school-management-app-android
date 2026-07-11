package com.school.app.ui.fees

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.razorpay.Checkout
import com.school.app.domain.model.Fee
import com.school.app.domain.model.PaymentInitiateResponse
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.formatMoney
import com.school.app.viewmodel.FeesViewModel
import com.school.app.viewmodel.PaymentUiState
import com.school.app.viewmodel.UiState
import org.json.JSONObject

@Composable
fun FeesScreen(
    role: Role,
    onBack: () -> Unit,
    viewModel: FeesViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        "Fees · ${viewModel.studentName}"
    } else {
        "Fees"
    }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val paymentState = viewModel.paymentState

    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentUiState.ReadyToOpen -> {
                val activity = context as? Activity
                if (activity != null) {
                    openRazorpayCheckout(activity, paymentState.order)
                }
                viewModel.onCheckoutOpened()
            }
            is PaymentUiState.Success -> {
                snackbarHostState.showSnackbar("Payment submitted. It may take a few minutes to reflect here.")
                viewModel.dismissPaymentMessage()
            }
            is PaymentUiState.Error -> {
                snackbarHostState.showSnackbar(paymentState.message)
                viewModel.dismissPaymentMessage()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = { AppTopBar(title, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> {
                    if (state.data.isEmpty()) {
                        EmptyState("No fee records yet")
                    } else {
                        FeesList(
                            fees = state.data,
                            canPay = role == Role.PARENT,
                            payingFeeId = (paymentState as? PaymentUiState.Initiating)?.feeId,
                            onPay = viewModel::payFee,
                        )
                    }
                }
            }
        }
    }
}

private fun openRazorpayCheckout(activity: Activity, order: PaymentInitiateResponse) {
    val checkout = Checkout()
    checkout.setKeyID(order.gatewayKeyId)
    val options = JSONObject().apply {
        put("name", "School Fee Payment")
        put("order_id", order.gatewayOrderId)
        put("currency", order.currency)
        put("amount", order.amountInSmallestUnit)
    }
    checkout.open(activity, options)
}

@Composable
private fun FeesList(
    fees: List<Fee>,
    canPay: Boolean,
    payingFeeId: String?,
    onPay: (String) -> Unit,
) {
    val totalDue = fees.sumOf { it.amountDue }
    val totalPaid = fees.sumOf { it.amountPaid }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    SummaryColumn("Total due", formatMoney(totalDue), Modifier.weight(1f))
                    SummaryColumn("Paid", formatMoney(totalPaid), Modifier.weight(1f))
                    SummaryColumn(
                        "Outstanding",
                        formatMoney((totalDue - totalPaid).coerceAtLeast(0.0)),
                        Modifier.weight(1f),
                    )
                }
            }
        }
        items(fees, key = { it.id }) { fee ->
            val outstanding = fee.amountDue - fee.amountPaid > 0 && fee.status.name != "PAID"
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fee.term,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(fee.status.name, fee.status.color())
                    }
                    Text(
                        "${formatMoney(fee.amountPaid)} paid of ${formatMoney(fee.amountDue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Due by ${formatDate(fee.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (canPay && outstanding) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (payingFeeId == fee.id) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            TextButton(onClick = { onPay(fee.id) }, enabled = payingFeeId == null) {
                                Text(if (payingFeeId == fee.id) "Opening…" else "Pay Now")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
