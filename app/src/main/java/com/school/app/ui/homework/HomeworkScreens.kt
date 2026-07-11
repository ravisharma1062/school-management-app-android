package com.school.app.ui.homework

import android.content.Intent
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.school.app.BuildConfig
import com.school.app.R
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkSubmission
import com.school.app.domain.model.HomeworkSubmissionStatus
import com.school.app.domain.model.Role
import com.school.app.domain.model.Student
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CacheBanner
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.DatePickerField
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.formatDate
import com.school.app.ui.common.stringRes
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
    val context = LocalContext.current

    // Refresh when returning from the create screen so new homework shows up.
    LifecycleResumeEffect(Unit) {
        if (viewModel.state.loadedFor != null) viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(state.loadedFor, role) {
        if (role == Role.PARENT && state.loadedFor != null) {
            viewModel.loadMyChildrenForCurrentClass()
        }
    }

    val openSubmissionTitle = stringRes(R.string.homework_open_intent)
    LaunchedEffect(viewModel.downloadedFile) {
        val file = viewModel.downloadedFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, openSubmissionTitle))
        viewModel.consumeDownloadedFile()
    }

    Scaffold(
        topBar = { AppTopBar(stringRes(R.string.homework_title), onBack) },
        floatingActionButton = {
            val loaded = state.loadedFor
            if (role == Role.TEACHER && loaded != null) {
                FloatingActionButton(onClick = { onCreate(loaded.first, loaded.second) }) {
                    Icon(Icons.Default.Add, contentDescription = stringRes(R.string.homework_post_fab))
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
                    label = { Text(stringRes(R.string.homework_class)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.section,
                    onValueChange = { viewModel.section = it },
                    label = { Text(stringRes(R.string.homework_section)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = viewModel::refresh,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) { Text(stringRes(R.string.homework_view)) }
            }

            CacheBanner(state.fromCache)

            when {
                state.loading -> CenteredLoading()
                state.error != null && state.items.isEmpty() && state.loadedFor != null ->
                    ErrorState(state.error, onRetry = viewModel::refresh)
                state.loadedFor == null ->
                    EmptyState(state.error ?: stringRes(R.string.homework_enter_class_section))
                state.items.isEmpty() -> EmptyState(stringRes(R.string.homework_none))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { hw ->
                        HomeworkCard(hw, role = role, viewModel = viewModel)
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
private fun HomeworkCard(homework: Homework, role: Role, viewModel: HomeworkListViewModel) {
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
                stringRes(R.string.homework_due, formatDate(homework.dueDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (role == Role.PARENT) {
                ParentSubmissionSection(homework, viewModel)
            } else if (role == Role.TEACHER) {
                TeacherSubmissionsSection(homework, viewModel)
            }
        }
    }
}

@Composable
private fun ParentSubmissionSection(homework: Homework, viewModel: HomeworkListViewModel) {
    if (viewModel.myChildrenInLoadedClass.isEmpty()) return

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    viewModel.submitError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        viewModel.myChildrenInLoadedClass.forEach { child ->
            ChildSubmissionRow(homework, child, viewModel)
        }
    }
}

@Composable
private fun ChildSubmissionRow(homework: Homework, child: Student, viewModel: HomeworkListViewModel) {
    val submission = viewModel.submissionFor(homework.id, child.id)
    val submitting = viewModel.submittingStudentId == child.id
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.submit(homework.id, child.id, uri)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(child.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (submission != null) {
            StatusChip(submission.status.name, submission.status.chipColor())
            TextButton(onClick = { viewModel.downloadSubmissionFile(submission) }) { Text(stringRes(R.string.common_view)) }
        } else {
            TextButton(onClick = { launcher.launch("*/*") }, enabled = !submitting) {
                Text(stringRes(if (submitting) R.string.homework_uploading else R.string.homework_submit))
            }
        }
    }
    submission?.grade?.let {
        Text(
            stringRes(R.string.homework_grade_prefix, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TeacherSubmissionsSection(homework: Homework, viewModel: HomeworkListViewModel) {
    val expanded = viewModel.expandedHomeworkIds[homework.id] == true

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    TextButton(onClick = { viewModel.toggleExpanded(homework.id) }) {
        Text(stringRes(if (expanded) R.string.homework_hide_submissions else R.string.homework_view_submissions))
    }

    if (expanded) {
        val submissions = viewModel.submissionsByHomeworkId[homework.id]
        when {
            submissions == null -> CircularProgressIndicator(Modifier.width(20.dp))
            submissions.isEmpty() -> Text(
                stringRes(R.string.homework_none_submissions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                submissions.forEach { submission ->
                    SubmissionGradingRow(homework.id, submission, viewModel)
                }
            }
        }
    }
}

@Composable
private fun SubmissionGradingRow(homeworkId: String, submission: HomeworkSubmission, viewModel: HomeworkListViewModel) {
    var grading by remember(submission.id) { mutableStateOf(false) }
    var grade by remember(submission.id) { mutableStateOf(submission.grade ?: "") }
    var feedback by remember(submission.id) { mutableStateOf(submission.teacherFeedback ?: "") }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(submission.fileName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            StatusChip(submission.status.name, submission.status.chipColor())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { viewModel.downloadSubmissionFile(submission) }) { Text(stringRes(R.string.homework_view_file)) }
            TextButton(onClick = { grading = !grading }) {
                Text(stringRes(if (submission.status == HomeworkSubmissionStatus.GRADED) R.string.homework_edit_grade else R.string.homework_grade))
            }
        }
        if (grading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text(stringRes(R.string.homework_grade)) },
                    singleLine = true,
                    modifier = Modifier.width(100.dp),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.grade(submission.id, homeworkId, grade, feedback.ifBlank { null })
                    grading = false
                }) { Text(stringRes(R.string.homework_save)) }
            }
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text(stringRes(R.string.homework_feedback_optional)) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        } else if (submission.grade != null) {
            Text(
                stringRes(R.string.homework_grade_prefix, submission.grade!!) + (submission.teacherFeedback?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun HomeworkSubmissionStatus.chipColor() = when (this) {
    HomeworkSubmissionStatus.SUBMITTED -> androidx.compose.ui.graphics.Color(0xFF0284C7)
    HomeworkSubmissionStatus.GRADED -> androidx.compose.ui.graphics.Color(0xFF16A34A)
}

@Composable
fun HomeworkCreateScreen(
    onDone: () -> Unit,
    viewModel: HomeworkCreateViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.created) {
        if (viewModel.created) onDone()
    }

    Scaffold(topBar = { AppTopBar(stringRes(R.string.homework_create_title), onDone) }) { padding ->
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
                    label = { Text(stringRes(R.string.homework_class)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.section,
                    onValueChange = { viewModel.section = it },
                    label = { Text(stringRes(R.string.homework_section)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.subject,
                onValueChange = { viewModel.subject = it },
                label = { Text(stringRes(R.string.homework_subject)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text(stringRes(R.string.homework_title_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text(stringRes(R.string.homework_description_optional)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DatePickerField(
                label = stringRes(R.string.homework_due_date),
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
                Text(stringRes(if (viewModel.submitting) R.string.homework_posting else R.string.homework_post_button))
            }
        }
    }
}
