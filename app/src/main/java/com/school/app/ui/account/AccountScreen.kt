package com.school.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.R
import com.school.app.domain.model.EntitlementDto
import com.school.app.domain.model.SchoolStatus
import com.school.app.domain.model.SubscriptionDto
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.featureLabel
import com.school.app.ui.common.formatDateTime
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.AccountViewModel
import com.school.app.viewmodel.UiState

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    Scaffold(topBar = { AppTopBar(stringRes(R.string.account_title), onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val s = state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(s.message, onRetry = viewModel::load)
                is UiState.Ready -> AccountContent(s.data)
            }
        }
    }
}

@Composable
private fun AccountContent(subscription: SubscriptionDto) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                stringRes(R.string.account_current_plan),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                subscription.planName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        StatusChip(statusLabel(subscription.status), subscription.status.color())
                    }
                    subscription.currentPeriodEnd?.let {
                        Text(
                            stringRes(R.string.account_renews, formatDateTime(it)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    subscription.trialEndsAt?.let {
                        Text(
                            stringRes(R.string.account_trial_ends, formatDateTime(it)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            Text(
                stringRes(R.string.account_entitlements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(subscription.entitlements, key = { it.featureKey }) { EntitlementRow(it) }
    }
}

@Composable
private fun EntitlementRow(entitlement: EntitlementDto) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(featureLabel(entitlement.featureKey), style = MaterialTheme.typography.bodyLarge)
                entitlement.limitValue?.let {
                    val usage = entitlement.currentUsage
                    Text(
                        if (usage != null) stringRes(R.string.account_usage_limit, usage, it) else stringRes(R.string.account_limit, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val includedColor = if (entitlement.enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            StatusChip(
                if (entitlement.enabled) stringRes(R.string.account_included) else stringRes(R.string.account_not_included),
                includedColor,
            )
        }
    }
}

@Composable
private fun statusLabel(status: SchoolStatus): String = when (status) {
    SchoolStatus.TRIAL -> stringRes(R.string.account_status_trial)
    SchoolStatus.ACTIVE -> stringRes(R.string.account_status_active)
    SchoolStatus.PAST_DUE -> stringRes(R.string.account_status_past_due)
    SchoolStatus.SUSPENDED -> stringRes(R.string.account_status_suspended)
    SchoolStatus.CANCELLED -> stringRes(R.string.account_status_cancelled)
}
