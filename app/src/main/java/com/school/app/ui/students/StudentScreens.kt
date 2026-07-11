package com.school.app.ui.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.R
import com.school.app.domain.model.Role
import com.school.app.domain.model.Student
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.stringRes
import com.school.app.ui.navigation.Routes
import com.school.app.viewmodel.ChildrenViewModel
import com.school.app.viewmodel.StudentDetailViewModel
import com.school.app.viewmodel.StudentsViewModel
import com.school.app.viewmodel.UiState

@Composable
fun StudentsScreen(
    onBack: () -> Unit,
    onStudentClick: (Student) -> Unit,
    viewModel: StudentsViewModel = hiltViewModel(),
) {
    val state = viewModel.state
    Scaffold(topBar = { AppTopBar(stringRes(R.string.students_title), onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = viewModel.nameFilter,
                    onValueChange = { viewModel.nameFilter = it },
                    label = { Text(stringRes(R.string.students_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.classFilter,
                    onValueChange = { viewModel.classFilter = it },
                    label = { Text(stringRes(R.string.students_class)) },
                    singleLine = true,
                    modifier = Modifier.width(90.dp),
                )
                Button(
                    onClick = viewModel::refresh,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) { Text(stringRes(R.string.students_search)) }
            }

            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> CenteredLoading()
                    state.error != null && state.items.isEmpty() ->
                        ErrorState(state.error, onRetry = viewModel::refresh)
                    state.items.isEmpty() -> EmptyState(stringRes(R.string.students_none_found))
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { student ->
                            StudentCard(student, onClick = { onStudentClick(student) })
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
}

@Composable
fun ChildrenScreen(
    onBack: () -> Unit,
    onChildClick: (Student) -> Unit,
    viewModel: ChildrenViewModel = hiltViewModel(),
) {
    Scaffold(topBar = { AppTopBar(stringRes(R.string.children_title), onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> {
                    if (state.data.isEmpty()) {
                        EmptyState(stringRes(R.string.children_none))
                    } else {
                        LazyColumn(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.data, key = { it.id }) { child ->
                                StudentCard(child, onClick = { onChildClick(child) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentCard(student: Student, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    student.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringRes(R.string.student_class_roll, student.studentClass, student.section, student.rollNo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun StudentDetailScreen(
    role: Role,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    Scaffold(topBar = { AppTopBar(stringRes(R.string.student_detail_title), onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val state = viewModel.state) {
                UiState.Loading -> CenteredLoading()
                is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                is UiState.Ready -> StudentDetailContent(state.data, role, onNavigate)
            }
        }
    }
}

@Composable
private fun StudentDetailContent(
    student: Student,
    role: Role,
    onNavigate: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    student.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringRes(R.string.student_class_roll, student.studentClass, student.section, student.rollNo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringRes(R.string.student_dob, formatDate(student.dob)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column {
                ActionRow(Icons.Default.ChecklistRtl, stringRes(R.string.action_attendance_history)) {
                    onNavigate(Routes.attendanceHistory(student.id, student.name))
                }
                HorizontalDivider()
                ActionRow(Icons.Default.WorkspacePremium, stringRes(R.string.action_exam_results)) {
                    onNavigate(Routes.results(student.id, student.name))
                }
                HorizontalDivider()
                ActionRow(Icons.Default.Schedule, stringRes(R.string.action_class_timetable)) {
                    onNavigate(Routes.timetable(student.studentClass, student.section))
                }
                HorizontalDivider()
                ActionRow(Icons.AutoMirrored.Filled.MenuBook, stringRes(R.string.action_homework)) {
                    onNavigate(Routes.homework(student.studentClass, student.section))
                }
                // Fee access is ADMIN/PARENT-only on the backend.
                if (role == Role.ADMIN || role == Role.PARENT) {
                    HorizontalDivider()
                    ActionRow(Icons.AutoMirrored.Filled.ReceiptLong, stringRes(R.string.action_fees)) {
                        onNavigate(Routes.fees(student.id, student.name))
                    }
                }
                if (role == Role.PARENT) {
                    HorizontalDivider()
                    ActionRow(Icons.Default.DirectionsBus, stringRes(R.string.action_bus_tracking)) {
                        onNavigate(Routes.transport(student.id, student.name))
                    }
                    HorizontalDivider()
                    ActionRow(Icons.Default.LibraryBooks, stringRes(R.string.action_library)) {
                        onNavigate(Routes.library(student.id, student.name))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}
