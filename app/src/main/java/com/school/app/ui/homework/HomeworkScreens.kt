package com.school.app.ui.homework

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.school.app.domain.model.Homework
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CacheBanner
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.DatePickerField
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.formatDate
import com.school.app.viewmodel.HomeworkCreateViewModel
import com.school.app.viewmodel.HomeworkListViewModel

@Composable
fun HomeworkListScreen(
    role: Role,
    onBack: () -> Unit,
    onCreate: (cls: String, sec: String) -> Unit,
    viewModel: HomeworkListViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    // Refresh when returning from the create screen so new homework shows up.
    LifecycleResumeEffect(Unit) {
        if (viewModel.state.loadedFor != null) viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { AppTopBar("Homework", onBack) },
        floatingActionButton = {
            val loaded = state.loadedFor
            if (role == Role.TEACHER && loaded != null) {
                FloatingActionButton(onClick = { onCreate(loaded.first, loaded.second) }) {
                    Icon(Icons.Default.Add, contentDescription = "Post homework")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = viewModel.studentClass,
                    onValueChange = { viewModel.studentClass = it },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.section,
                    onValueChange = { viewModel.section = it },
                    label = { Text("Section") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = viewModel::refresh,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) { Text("View") }
            }

            CacheBanner(state.fromCache)

            when {
                state.loading -> CenteredLoading()
                state.error != null && state.items.isEmpty() && state.loadedFor != null ->
                    ErrorState(state.error, onRetry = viewModel::refresh)
                state.loadedFor == null ->
                    EmptyState(state.error ?: "Enter a class and section to view homework")
                state.items.isEmpty() -> EmptyState("No homework posted yet")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { HomeworkCard(it) }
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
private fun HomeworkCard(homework: Homework) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    homework.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(homework.subject, MaterialTheme.colorScheme.primary)
            }
            homework.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "Due ${formatDate(homework.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun HomeworkCreateScreen(
    onDone: () -> Unit,
    viewModel: HomeworkCreateViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.created) {
        if (viewModel.created) onDone()
    }

    Scaffold(topBar = { AppTopBar("Post Homework", onDone) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.studentClass,
                    onValueChange = { viewModel.studentClass = it },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.section,
                    onValueChange = { viewModel.section = it },
                    label = { Text("Section") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.subject,
                onValueChange = { viewModel.subject = it },
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Description (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DatePickerField(
                label = "Due date",
                date = viewModel.dueDate,
                onDateChange = { viewModel.dueDate = it },
                modifier = Modifier.fillMaxWidth(),
            )

            viewModel.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = !viewModel.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(if (viewModel.submitting) "Posting…" else "Post homework")
            }
        }
    }
}
