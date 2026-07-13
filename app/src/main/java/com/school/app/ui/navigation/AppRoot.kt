package com.school.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.app.data.auth.Session
import com.school.app.domain.model.Role
import com.school.app.ui.account.AccountScreen
import com.school.app.ui.account.PastDueBanner
import com.school.app.ui.account.SuspendedScreen
import com.school.app.ui.attendance.AttendanceHistoryScreen
import com.school.app.ui.attendance.AttendanceMarkScreen
import com.school.app.ui.auth.LoginScreen
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.LocalLanguage
import com.school.app.ui.event.EventCreateScreen
import com.school.app.ui.event.EventsListScreen
import com.school.app.ui.examresult.ExamResultsScreen
import com.school.app.ui.fees.FeesScreen
import com.school.app.ui.home.HomeScreen
import com.school.app.ui.homework.HomeworkCreateScreen
import com.school.app.ui.homework.HomeworkListScreen
import com.school.app.ui.leaverequest.LeaveRequestsScreen
import com.school.app.ui.library.LibraryCatalogScreen
import com.school.app.ui.library.LibraryScreen
import com.school.app.ui.messaging.ConversationThreadScreen
import com.school.app.ui.messaging.MessagesListScreen
import com.school.app.ui.notices.NoticesScreen
import com.school.app.ui.students.ChildrenScreen
import com.school.app.ui.students.StudentDetailScreen
import com.school.app.ui.students.StudentsScreen
import com.school.app.ui.timetable.TimetableScreen
import com.school.app.ui.transport.TransportScreen
import com.school.app.viewmodel.MainViewModel
import com.school.app.viewmodel.SessionUi

object Routes {
    const val HOME = "home"
    const val STUDENTS = "students"
    const val CHILDREN = "children"
    const val STUDENT_DETAIL = "student/{studentId}"
    const val ATTENDANCE_MARK = "attendance/mark"
    const val ATTENDANCE_HISTORY = "attendance/history/{studentId}?name={name}"
    const val TIMETABLE = "timetable?cls={cls}&sec={sec}"
    const val HOMEWORK = "homework?cls={cls}&sec={sec}"
    const val HOMEWORK_CREATE = "homework/create?cls={cls}&sec={sec}"
    const val RESULTS = "results/{studentId}?name={name}"
    const val FEES = "fees/{studentId}?name={name}"
    const val NOTICES = "notices"
    const val LEAVE_REQUESTS = "leave-requests"
    const val MESSAGES = "messages"
    const val CONVERSATION_THREAD = "conversations/{conversationId}?otherName={otherName}"
    const val EVENTS = "events"
    const val EVENT_CREATE = "events/create"
    const val TRANSPORT = "transport/{studentId}?name={name}"
    const val LIBRARY = "library/{studentId}?name={name}"
    const val LIBRARY_CATALOG = "library/catalog"
    const val ACCOUNT = "account"

    fun studentDetail(id: String) = "student/$id"
    fun attendanceHistory(id: String, name: String) =
        "attendance/history/$id?name=${Uri.encode(name)}"
    fun timetable(cls: String = "", sec: String = "") =
        "timetable?cls=${Uri.encode(cls)}&sec=${Uri.encode(sec)}"
    fun homework(cls: String = "", sec: String = "") =
        "homework?cls=${Uri.encode(cls)}&sec=${Uri.encode(sec)}"
    fun homeworkCreate(cls: String, sec: String) =
        "homework/create?cls=${Uri.encode(cls)}&sec=${Uri.encode(sec)}"
    fun results(id: String, name: String) = "results/$id?name=${Uri.encode(name)}"
    fun fees(id: String, name: String) = "fees/$id?name=${Uri.encode(name)}"
    fun conversationThread(id: String, otherName: String) =
        "conversations/$id?otherName=${Uri.encode(otherName)}"
    fun transport(id: String, name: String) = "transport/$id?name=${Uri.encode(name)}"
    fun library(id: String, name: String) = "library/$id?name=${Uri.encode(name)}"
}

@Composable
fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()
    val isSuspended by mainViewModel.isSuspended.collectAsStateWithLifecycle()
    val isPastDue by mainViewModel.isPastDue.collectAsStateWithLifecycle()
    when (val s = sessionState) {
        SessionUi.Loading -> CenteredLoading()
        SessionUi.LoggedOut -> LoginScreen()
        is SessionUi.LoggedIn -> {
            if (isSuspended) {
                SuspendedScreen(onLogOut = mainViewModel::logout)
            } else {
                MainNavHost(
                    session = s.session,
                    onLogout = mainViewModel::logout,
                    isPastDue = isPastDue,
                    onDismissPastDue = mainViewModel::dismissPastDueBanner,
                )
            }
        }
    }
}

@Composable
private fun MainNavHost(
    session: Session,
    onLogout: () -> Unit,
    isPastDue: Boolean,
    onDismissPastDue: () -> Unit,
) {
    val navController = rememberNavController()
    val role = session.role

    fun optionalString(name: String) = navArgument(name) {
        type = NavType.StringType
        defaultValue = ""
    }

    CompositionLocalProvider(LocalLanguage provides session.preferredLanguage) {
    Column {
    if (isPastDue) {
        PastDueBanner(onDismiss = onDismissPastDue)
    }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                session = session,
                onNavigate = { navController.navigate(it) },
                onLogout = onLogout,
            )
        }
        composable(Routes.STUDENTS) {
            StudentsScreen(
                onBack = { navController.popBackStack() },
                onStudentClick = { navController.navigate(Routes.studentDetail(it.id)) },
            )
        }
        composable(Routes.CHILDREN) {
            ChildrenScreen(
                onBack = { navController.popBackStack() },
                onChildClick = { navController.navigate(Routes.studentDetail(it.id)) },
            )
        }
        composable(Routes.STUDENT_DETAIL) {
            StudentDetailScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
            )
        }
        composable(Routes.ATTENDANCE_MARK) {
            AttendanceMarkScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.ATTENDANCE_HISTORY,
            arguments = listOf(optionalString("name")),
        ) {
            AttendanceHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.TIMETABLE,
            arguments = listOf(optionalString("cls"), optionalString("sec")),
        ) {
            TimetableScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.HOMEWORK,
            arguments = listOf(optionalString("cls"), optionalString("sec")),
        ) {
            HomeworkListScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onCreate = { cls, sec -> navController.navigate(Routes.homeworkCreate(cls, sec)) },
            )
        }
        composable(
            Routes.HOMEWORK_CREATE,
            arguments = listOf(optionalString("cls"), optionalString("sec")),
        ) {
            HomeworkCreateScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.RESULTS,
            arguments = listOf(optionalString("name")),
        ) {
            ExamResultsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.FEES,
            arguments = listOf(optionalString("name")),
        ) {
            FeesScreen(role = role, onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTICES) {
            NoticesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEAVE_REQUESTS) {
            LeaveRequestsScreen(role = role, onBack = { navController.popBackStack() })
        }
        composable(Routes.MESSAGES) {
            MessagesListScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversation ->
                    val otherName = if (role == Role.TEACHER) conversation.parentName else conversation.teacherName
                    navController.navigate(Routes.conversationThread(conversation.id, otherName))
                },
            )
        }
        composable(
            Routes.CONVERSATION_THREAD,
            arguments = listOf(optionalString("otherName")),
        ) { backStackEntry ->
            val otherName = backStackEntry.arguments?.getString("otherName")?.ifBlank { null } ?: "Conversation"
            ConversationThreadScreen(
                title = otherName,
                currentUserId = session.userId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.EVENTS) {
            EventsListScreen(
                role = role,
                onBack = { navController.popBackStack() },
                onCreate = { navController.navigate(Routes.EVENT_CREATE) },
            )
        }
        composable(Routes.EVENT_CREATE) {
            EventCreateScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.TRANSPORT,
            arguments = listOf(optionalString("name")),
        ) {
            TransportScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.LIBRARY,
            arguments = listOf(optionalString("name")),
        ) {
            LibraryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIBRARY_CATALOG) {
            LibraryCatalogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(onBack = { navController.popBackStack() })
        }
    }
    }
    }
}
