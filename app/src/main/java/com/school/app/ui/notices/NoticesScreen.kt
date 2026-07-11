package com.school.app.ui.notices

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.R
import com.school.app.domain.model.Notice
import com.school.app.domain.model.TargetRole
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.formatDateTime
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.NoticesViewModel

@Composable
fun NoticesScreen(
    onBack: () -> Unit,
    viewModel: NoticesViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    Scaffold(topBar = { AppTopBar(stringRes(R.string.notices_title), onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.loading -> CenteredLoading()
                state.error != null && state.items.isEmpty() ->
                    ErrorState(state.error, onRetry = viewModel::refresh)
                state.items.isEmpty() -> EmptyState(stringRes(R.string.notices_none))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { NoticeCard(it) }
                    if (!state.endReached) {
                        item {
                            LaunchedEffect(state.items.size) { viewModel.loadNext() }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: Notice) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (notice.targetRole != TargetRole.ALL) {
                    StatusChip(notice.targetRole.name, MaterialTheme.colorScheme.secondary)
                }
            }
            notice.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                formatDateTime(notice.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
