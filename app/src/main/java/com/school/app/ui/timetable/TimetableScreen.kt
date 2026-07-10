package com.school.app.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.domain.model.TimetableEntry
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CacheBanner
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.viewmodel.TimetableViewModel
import com.school.app.viewmodel.UiState

private val dayOrder = listOf(
    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY",
)

@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    viewModel: TimetableViewModel = hiltViewModel(),
) {
    Scaffold(topBar = { AppTopBar("Timetable", onBack) }) { padding ->
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
                    onClick = viewModel::load,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically),
                ) { Text("View") }
            }

            when (val state = viewModel.state) {
                null -> EmptyState("Enter a class and section to view the timetable")
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> {
                    CacheBanner(state.fromCache)
                    if (state.data.isEmpty()) {
                        EmptyState("No timetable published for this class yet")
                    } else {
                        TimetableList(state.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableList(entries: List<TimetableEntry>) {
    val byDay = entries.groupBy { it.dayOfWeek.uppercase() }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dayOrder.filter { byDay.containsKey(it) }.forEach { day ->
            item(key = day) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            day.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        byDay.getValue(day).sortedBy { it.period }.forEach { entry ->
                            Row(Modifier.padding(top = 8.dp)) {
                                Text(
                                    "P${entry.period}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp),
                                )
                                Text(
                                    entry.subject,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
