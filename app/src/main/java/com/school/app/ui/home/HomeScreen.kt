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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Celebration
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
import com.school.app.data.auth.Session
import com.school.app.domain.model.LanguageCode
import com.school.app.domain.model.Role
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.t
import com.school.app.ui.navigation.Routes
import com.school.app.viewmodel.HomeViewModel

private data class Feature(val titleKey: String, val icon: ImageVector, val route: String)

private fun featuresFor(role: Role): List<Feature> = when (role) {
    Role.TEACHER -> listOf(
        Feature("feature.markAttendance", Icons.Default.ChecklistRtl, Routes.ATTENDANCE_MARK),
        Feature("feature.students", Icons.Default.Groups, Routes.STUDENTS),
        Feature("feature.timetable", Icons.Default.Schedule, Routes.timetable()),
        Feature("feature.homework", Icons.AutoMirrored.Filled.MenuBook, Routes.homework()),
        Feature("feature.notices", Icons.Default.Campaign, Routes.NOTICES),
        Feature("feature.leaveRequests", Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
        Feature("feature.events", Icons.Default.Celebration, Routes.EVENTS),
        Feature("feature.messages", Icons.AutoMirrored.Filled.Chat, Routes.MESSAGES),
    )
    Role.PARENT -> listOf(
        Feature("feature.myChildren", Icons.Default.EscalatorWarning, Routes.CHILDREN),
        Feature("feature.notices", Icons.Default.Campaign, Routes.NOTICES),
        Feature("feature.leaveRequests", Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
        Feature("feature.events", Icons.Default.Celebration, Routes.EVENTS),
        Feature("feature.messages", Icons.AutoMirrored.Filled.Chat, Routes.MESSAGES),
    )
    Role.ADMIN -> listOf(
        Feature("feature.students", Icons.Default.Groups, Routes.STUDENTS),
        Feature("feature.timetable", Icons.Default.Schedule, Routes.timetable()),
        Feature("feature.homework", Icons.AutoMirrored.Filled.MenuBook, Routes.homework()),
        Feature("feature.notices", Icons.Default.Campaign, Routes.NOTICES),
        Feature("feature.leaveRequests", Icons.Default.BeachAccess, Routes.LEAVE_REQUESTS),
        Feature("feature.events", Icons.Default.Celebration, Routes.EVENTS),
    )
}

@Composable
fun HomeScreen(
    session: Session,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val pendingCount by viewModel.pendingAttendanceCount.collectAsStateWithLifecycle()
    val lang = session.preferredLanguage

    NotificationPermissionRequest()

    Scaffold(
        topBar = {
            AppTopBar(title = t(lang, "app.title")) {
                LanguageSwitcher(current = lang, onChange = viewModel::setLanguage)
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = t(lang, "common.logOut"))
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
                t(lang, "home.hello", session.userName ?: session.userEmail ?: "there"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                t(lang, "role.${session.role.name}"),
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
                            t(lang, "common.pendingSync", pendingCount, if (pendingCount == 1) "" else "s"),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = viewModel::syncNow) { Text(t(lang, "common.syncNow")) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(featuresFor(session.role)) { feature ->
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
                                t(lang, feature.titleKey),
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
