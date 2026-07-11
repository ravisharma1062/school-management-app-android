package com.school.app.ui.examresult

import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.BuildConfig
import com.school.app.R
import com.school.app.domain.model.ExamResult
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.ErrorState
import com.school.app.ui.common.StatusChip
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.ExamResultsViewModel
import com.school.app.viewmodel.UiState

@Composable
fun ExamResultsScreen(
    onBack: () -> Unit,
    viewModel: ExamResultsViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        stringRes(R.string.exam_results_title_named, viewModel.studentName)
    } else {
        stringRes(R.string.exam_results_title)
    }
    val context = LocalContext.current
    val shareChooserTitle = stringRes(R.string.exam_results_share_intent)

    LaunchedEffect(viewModel.downloadedFile) {
        val file = viewModel.downloadedFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, shareChooserTitle))
        viewModel.consumeDownloadedFile()
    }

    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                viewModel.downloadError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = viewModel::downloadReportCard,
                    enabled = !viewModel.downloading,
                ) { Text(stringRes(if (viewModel.downloading) R.string.exam_results_downloading else R.string.exam_results_download)) }
            }

            Box(Modifier.weight(1f)) {
                when (val state = viewModel.state) {
                    UiState.Loading -> CenteredLoading()
                    is UiState.Error -> ErrorState(state.message, onRetry = viewModel::load)
                    is UiState.Ready -> {
                        if (state.data.isEmpty()) {
                            EmptyState(stringRes(R.string.exam_results_none))
                        } else {
                            ResultsList(state.data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsList(results: List<ExamResult>) {
    val byTerm = results.groupBy { it.term }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byTerm.forEach { (term, termResults) ->
            item(key = "term-$term") {
                Text(
                    term,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(termResults, key = { it.id }) { result ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                result.subject,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                result.examName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${result.marksObtained.trimZeros()} / ${result.maxMarks.trimZeros()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            StatusChip(
                                result.grade,
                                MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Double.trimZeros(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()
