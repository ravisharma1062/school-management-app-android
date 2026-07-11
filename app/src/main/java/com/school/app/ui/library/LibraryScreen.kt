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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.school.app.domain.model.Book
import com.school.app.domain.model.BookIssue
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.formatMoney
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.LibraryCatalogViewModel
import com.school.app.viewmodel.LibraryViewModel
import com.school.app.viewmodel.UiState

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        stringRes(R.string.library_title_named, viewModel.studentName)
    } else {
        stringRes(R.string.library_title)
    }

    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> {
                    if (state.data.isEmpty()) {
                        EmptyState(stringRes(R.string.library_none))
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
                        stringRes(R.string.library_issued_due, formatDate(issue.issuedAt), formatDate(issue.dueDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    issue.returnedAt?.let {
                        Text(
                            stringRes(R.string.library_returned, formatDate(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if ((issue.fineAmount ?: 0.0) > 0.0) {
                        Text(
                            stringRes(R.string.library_fine, formatMoney(issue.fineAmount!!)),
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

@Composable
fun LibraryCatalogScreen(
    onBack: () -> Unit,
    viewModel: LibraryCatalogViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    Scaffold(topBar = { AppTopBar(stringRes(R.string.library_catalog_title), onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = viewModel.search,
                onValueChange = {
                    viewModel.search = it
                    viewModel.refresh()
                },
                label = { Text(stringRes(R.string.library_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )

            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> CenteredLoading()
                    state.error != null && state.items.isEmpty() ->
                        ErrorState(state.error, onRetry = viewModel::refresh)
                    state.items.isEmpty() ->
                        EmptyState(
                            stringRes(if (viewModel.search.isBlank()) R.string.library_search_prompt else R.string.library_search_none),
                        )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { book -> BookCard(book) }
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
}

@Composable
private fun BookCard(book: Book) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringRes(R.string.library_available, book.availableCopies, book.totalCopies),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
