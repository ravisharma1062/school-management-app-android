package com.school.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.attendancePercentage
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CacheBanner
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.DatePickerField
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.formatDate
import com.school.app.viewmodel.AttendanceHistoryViewModel
import com.school.app.viewmodel.AttendanceMarkViewModel
import com.school.app.viewmodel.UiState

private val statusLabels = mapOf(
    AttendanceStatus.PRESENT to "P",
    AttendanceStatus.ABSENT to "A",
    AttendanceStatus.LATE to "L",
    AttendanceStatus.EXCUSED to "E",
)

@Composable
fun AttendanceMarkScreen(
    onBack: () -> Unit,
    viewModel: AttendanceMarkViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val roster = viewModel.roster

    viewModel.resultMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearResultMessage()
        }
    }

    Scaffold(
        topBar = { AppTopBar("Mark Attendance", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (roster is AttendanceMarkViewModel.Roster.Ready) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !viewModel.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(if (viewModel.submitting) "Saving…" else "Save attendance")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (pendingCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$pendingCount queued offline",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = viewModel::syncNow) { Text("Sync") }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
            }
            DatePickerField(
                label = "Date",
                date = viewModel.date,
                onDateChange = { viewModel.date = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Button(
                onClick = viewModel::loadRoster,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) { Text("Load students") }

            when (roster) {
                AttendanceMarkViewModel.Roster.Idle -> {}
                AttendanceMarkViewModel.Roster.Loading -> CenteredLoading()
                is AttendanceMarkViewModel.Roster.Error -> ErrorState(roster.message)
                is AttendanceMarkViewModel.Roster.Ready -> {
                    if (roster.alreadyMarked) {
                        Text(
                            "Attendance already exists for this date — saved marks are pre-selected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(roster.students, key = { it.id }) { student ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "${student.rollNo} · ${student.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        AttendanceStatus.entries.forEach { status ->
                                            FilterChip(
                                                selected = viewModel.statuses[student.id] == status,
                                                onClick = { viewModel.setStatus(student.id, status) },
                                                label = { Text(statusLabels.getValue(status)) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceHistoryScreen(
    onBack: () -> Unit,
    viewModel: AttendanceHistoryViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        "Attendance · ${viewModel.studentName}"
    } else {
        "Attendance"
    }
    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> Column {
                    CacheBanner(state.fromCache)
                    if (state.data.isEmpty()) {
                        EmptyState("No attendance records yet")
                    } else {
                        val records = state.data.sortedByDescending { it.date }
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            "${attendancePercentage(records)}% attendance",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 8.dp),
                                        ) {
                                            AttendanceStatus.entries.forEach { status ->
                                                val count = records.count { it.status == status }
                                                if (count > 0) {
                                                    StatusChip(
                                                        "${statusLabels.getValue(status)} $count",
                                                        status.color(),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            items(records, key = { it.id }) { record ->
                                Card(Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            formatDate(record.date),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f),
                                        )
                                        StatusChip(record.status.name, record.status.color())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
