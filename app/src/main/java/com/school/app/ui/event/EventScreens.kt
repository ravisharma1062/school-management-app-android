package com.school.app.ui.event

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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.school.app.R
import com.school.app.domain.model.Role
import com.school.app.domain.model.RsvpStatus
import com.school.app.domain.model.SchoolEvent
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.DatePickerField
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.EventCreateViewModel
import com.school.app.viewmodel.EventsListViewModel

@Composable
fun EventsListScreen(
    role: Role,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    viewModel: EventsListViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    val isAdmin = role == Role.ADMIN

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { AppTopBar(stringRes(R.string.events_title), onBack) },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = stringRes(R.string.events_add_fab))
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                state.loading && state.items.isEmpty() -> CenteredLoading()
                state.error != null && state.items.isEmpty() -> ErrorState(state.error, onRetry = viewModel::refresh)
                state.items.isEmpty() -> EmptyState(stringRes(R.string.events_none))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { event ->
                        EventCard(event, isAdmin, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: SchoolEvent, isAdmin: Boolean, viewModel: EventsListViewModel) {
    val rsvping = viewModel.rsvpingEventId == event.id
    val expanded = viewModel.expandedRsvpsEventIds[event.id] == true

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                event.myRsvpStatus?.let { StatusChip(rsvpLabel(it), it.chipColor()) }
            }
            event.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                stringRes(R.string.events_date_location, formatDate(event.eventDate)) + (event.location?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RsvpStatus.GOING, RsvpStatus.MAYBE, RsvpStatus.NOT_GOING).forEach { status ->
                    val selected = event.myRsvpStatus == status
                    TextButton(
                        onClick = { viewModel.rsvp(event.id, status) },
                        enabled = !rsvping,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text(rsvpLabel(status)) }
                }
            }

            if (isAdmin) {
                TextButton(onClick = { viewModel.toggleRsvps(event.id) }) {
                    Text(stringRes(if (expanded) R.string.events_hide_rsvps else R.string.events_view_rsvps))
                }
                if (expanded) {
                    val rsvps = viewModel.rsvpsByEventId[event.id]
                    when {
                        rsvps == null -> CircularProgressIndicator(Modifier.size(20.dp))
                        rsvps.isEmpty() -> Text(
                            stringRes(R.string.events_none_responses),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            rsvps.forEach { r ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.userName, style = MaterialTheme.typography.bodySmall)
                                    StatusChip(rsvpLabel(r.status), r.status.chipColor())
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
private fun rsvpLabel(status: RsvpStatus): String = when (status) {
    RsvpStatus.GOING -> stringRes(R.string.events_going)
    RsvpStatus.MAYBE -> stringRes(R.string.events_maybe)
    RsvpStatus.NOT_GOING -> stringRes(R.string.events_cant_go)
}

private fun RsvpStatus.chipColor() = when (this) {
    RsvpStatus.GOING -> Color(0xFF16A34A)
    RsvpStatus.MAYBE -> Color(0xFFCA8A04)
    RsvpStatus.NOT_GOING -> Color(0xFFDC2626)
}

@Composable
fun EventCreateScreen(
    onDone: () -> Unit,
    viewModel: EventCreateViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.created) {
        if (viewModel.created) onDone()
    }

    Scaffold(topBar = { AppTopBar(stringRes(R.string.events_create_title), onDone) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text(stringRes(R.string.events_title_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DatePickerField(
                label = stringRes(R.string.events_date),
                date = viewModel.eventDate,
                onDateChange = { viewModel.eventDate = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.location,
                onValueChange = { viewModel.location = it },
                label = { Text(stringRes(R.string.events_location_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text(stringRes(R.string.events_description_optional)) },
                minLines = 3,
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
                Text(stringRes(if (viewModel.submitting) R.string.events_creating else R.string.events_create_button))
            }
        }
    }
}
