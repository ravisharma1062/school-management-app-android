package com.school.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.EscalatorWarning
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.school.app.R
import com.school.app.data.auth.Session
import com.school.app.domain.model.FeatureKey
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.roleLabel
import com.school.app.ui.common.stringRes
import com.school.app.ui.navigation.Routes
import com.school.app.viewmodel.HomeViewModel

private data class Feature(
    @androidx.annotation.StringRes val titleRes: Int,
    val icon: ImageVector,
    val route: String,
    /** Hidden when non-null and not present in the caller's entitled-feature set. */
    val featureKey: FeatureKey? = null,
)

private fun featuresFor(role: Role, entitledFeatures: Set<FeatureKey>?): List<Feature> {
    val all = when (role) {
        Role.TEACHER -> listOf(
            Feature(R.string.feature_mark_attendance, Icons.Default.ChecklistRtl, Routes.ATTENDANCE_MARK),
            Feature(R.string.feature_students, Icons.Default.Groups, Routes.STUDENTS),
            Feature(R.string.feature_timetable, Icons.Default.Schedule, Routes.timetable()),
            Feature(R.string.feature_homework, Icons.AutoMirrored.Filled.MenuBook, Routes.homework()),
            Feature(R.string.feature_notices, Icons.Default.Campaign, Routes.NOTICES),
            Feature(R.string.feature_leave_requests, Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
            Feature(R.string.feature_events, Icons.Default.Celebration, Routes.EVENTS),
            Feature(R.string.feature_messages, Icons.AutoMirrored.Filled.Chat, Routes.MESSAGES, FeatureKey.MESSAGING),
            Feature(R.string.action_library, Icons.AutoMirrored.Filled.LibraryBooks, Routes.LIBRARY_CATALOG, FeatureKey.LIBRARY),
        )
        Role.PARENT -> listOf(
            Feature(R.string.feature_my_children, Icons.Default.EscalatorWarning, Routes.CHILDREN),
            Feature(R.string.feature_notices, Icons.Default.Campaign, Routes.NOTICES),
            Feature(R.string.feature_leave_requests, Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
            Feature(R.string.feature_events, Icons.Default.Celebration, Routes.EVENTS),
            Feature(R.string.feature_messages, Icons.AutoMirrored.Filled.Chat, Routes.MESSAGES, FeatureKey.MESSAGING),
            Feature(R.string.action_library, Icons.AutoMirrored.Filled.LibraryBooks, Routes.LIBRARY_CATALOG, FeatureKey.LIBRARY),
        )
        Role.ADMIN -> listOf(
            Feature(R.string.feature_students, Icons.Default.Groups, Routes.STUDENTS),
            Feature(R.string.feature_timetable, Icons.Default.Schedule, Routes.timetable()),
            Feature(R.string.feature_homework, Icons.AutoMirrored.Filled.MenuBook, Routes.homework()),
            Feature(R.string.feature_notices, Icons.Default.Campaign, Routes.NOTICES),
            Feature(R.string.feature_leave_requests, Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
            Feature(R.string.feature_events, Icons.Default.Celebration, Routes.EVENTS),
            Feature(R.string.action_library, Icons.AutoMirrored.Filled.LibraryBooks, Routes.LIBRARY_CATALOG, FeatureKey.LIBRARY),
            Feature(R.string.feature_account, Icons.Default.Settings, Routes.ACCOUNT),
        )
    }
    return if (entitledFeatures == null) {
        all
    } else {
        all.filter { it.featureKey == null || it.featureKey in entitledFeatures }
    }
}

@Composable
fun HomeScreen(
    session: Session,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val pendingCount by viewModel.pendingAttendanceCount.collectAsStateWithLifecycle()
    val entitledFeatures by viewModel.entitledFeatures.collectAsStateWithLifecycle()
    val lang = session.preferredLanguage

    NotificationPermissionRequest()

    Scaffold(
        topBar = {
            AppTopBar(title = stringRes(R.string.home_app_title)) {
                LanguageSwitcher(current = lang, onChange = viewModel::setLanguage)
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringRes(R.string.home_log_out))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                stringRes(R.string.home_hello, session.userName ?: session.userEmail ?: "there"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                roleLabel(session.role.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (pendingCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringRes(R.string.home_pending_sync, pendingCount),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = viewModel::syncNow) { Text(stringRes(R.string.home_sync_now)) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(featuresFor(session.role, entitledFeatures)) { feature ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(feature.route) },
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Icon(
                                feature.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                stringRes(feature.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitcher(current: LanguageCode, onChange: (LanguageCode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(LanguageCode.EN) }, enabled = current != LanguageCode.EN) {
            Text("EN", fontWeight = if (current == LanguageCode.EN) FontWeight.Bold else FontWeight.Normal)
        }
        TextButton(onClick = { onChange(LanguageCode.HI) }, enabled = current != LanguageCode.HI) {
            Text("हि", fontWeight = if (current == LanguageCode.HI) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

/** Asks for POST_NOTIFICATIONS once on Android 13+, for FCM notices. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
