package com.school.app.ui.library

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
import com.school.app.domain.model.BookIssue
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.formatMoney
import com.school.app.viewmodel.LibraryViewModel
import com.school.app.viewmodel.UiState

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        "Library · ${viewModel.studentName}"
    } else {
        "Library"
    }

    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> {
                    if (state.data.isEmpty()) {
                        EmptyState("No books issued yet")
                    } else {
                        IssuesList(state.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun IssuesList(issues: List<BookIssue>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(issues, key = { it.id }) { issue ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            issue.bookTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(issue.status.name, issue.status.color())
                    }
                    Text(
                        "Issued ${formatDate(issue.issuedAt)} · Due ${formatDate(issue.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    issue.returnedAt?.let {
                        Text(
                            "Returned ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if ((issue.fineAmount ?: 0.0) > 0.0) {
                        Text(
                            "Fine: ${formatMoney(issue.fineAmount!!)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
