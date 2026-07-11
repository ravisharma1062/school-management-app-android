package com.school.app.ui.leaverequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.school.app.domain.model.LeaveRequest
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.LeaveType
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.DatePickerField
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.color
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.LeaveRequestsViewModel
import com.school.app.viewmodel.canReviewLeaveRequests

@Composable
fun LeaveRequestsScreen(
    role: Role,
    onBack: () -> Unit,
    viewModel: LeaveRequestsViewModel = hiltViewModel(),
) {
    val isAdmin = canReviewLeaveRequests(role)
    val state = viewModel.state

    Scaffold(topBar = { AppTopBar(stringRes(R.string.leave_requests_title), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            if (!isAdmin) {
                SubmitLeaveForm(viewModel)
            }

            StatusFilterRow(
                selected = viewModel.statusFilter,
                onSelect = {
                    viewModel.statusFilter = it
                    viewModel.refresh()
                },
            )

            when {
                state.loading -> CenteredLoading()
                state.error != null && state.items.isEmpty() ->
                    ErrorState(state.error, onRetry = viewModel::refresh)
                state.items.isEmpty() -> EmptyState(
                    stringRes(if (isAdmin) R.string.leave_none_admin else R.string.leave_none_user),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { request ->
                        LeaveRequestCard(
                            request = request,
                            isAdmin = isAdmin,
                            reviewing = viewModel.reviewingId == request.id,
                            onApprove = { viewModel.review(request.id, LeaveStatus.APPROVED) },
                            onReject = { viewModel.review(request.id, LeaveStatus.REJECTED) },
                        )
                    }
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
private fun StatusFilterRow(selected: LeaveStatus?, onSelect: (LeaveStatus?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(stringRes(R.string.leave_status_all)) })
        LeaveStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.name) },
            )
        }
    }
}

@Composable
private fun SubmitLeaveForm(viewModel: LeaveRequestsViewModel) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringRes(R.string.leave_submit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeaveType.entries.forEach { t ->
                    FilterChip(
                        selected = viewModel.type == t,
                        onClick = { viewModel.type = t },
                        label = { Text(t.name) },
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DatePickerField(
                    label = stringRes(R.string.leave_from),
                    date = viewModel.fromDate,
                    onDateChange = { viewModel.fromDate = it },
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    label = stringRes(R.string.leave_to),
                    date = viewModel.toDate,
                    onDateChange = { viewModel.toDate = it },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = viewModel.reason,
                onValueChange = { viewModel.reason = it },
                label = { Text(stringRes(R.string.leave_reason_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                minLines = 2,
            )
            viewModel.submitError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (viewModel.submitSuccess) {
                Text(
                    stringRes(R.string.leave_submitted),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = viewModel::submit,
                enabled = !viewModel.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) { Text(stringRes(if (viewModel.submitting) R.string.leave_submitting else R.string.leave_submit_request)) }
        }
    }
}

@Composable
private fun LeaveRequestCard(
    request: LeaveRequest,
    isAdmin: Boolean,
    reviewing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    request.type.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(request.status.name, request.status.color())
            }
            Text(
                "${formatDate(request.fromDate)} – ${formatDate(request.toDate)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            request.reason?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (isAdmin && request.status == LeaveStatus.PENDING) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onApprove, enabled = !reviewing) { Text(stringRes(R.string.leave_approve)) }
                    Button(onClick = onReject, enabled = !reviewing) { Text(stringRes(R.string.leave_reject)) }
                }
            }
        }
    }
}
