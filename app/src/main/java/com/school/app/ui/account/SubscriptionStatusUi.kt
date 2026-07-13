package com.school.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.school.app.R
import com.school.app.ui.common.stringRes

/** Full-screen block shown instead of the app when the backend returns 403 SUBSCRIPTION_SUSPENDED. */
@Composable
fun SuspendedScreen(onLogOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                .padding(20.dp),
        ) {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Text(
            stringRes(R.string.subscription_suspended_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringRes(R.string.subscription_suspended_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(onClick = onLogOut, modifier = Modifier.padding(top = 20.dp)) {
            Text(stringRes(R.string.home_log_out))
        }
    }
}

/** Dismissible warning shown when a response carried X-Subscription-Status: PAST_DUE. */
@Composable
fun PastDueBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                stringRes(R.string.subscription_past_due_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringRes(R.string.common_cancel),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
