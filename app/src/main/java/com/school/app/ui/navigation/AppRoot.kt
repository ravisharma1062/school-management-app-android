package com.school.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.school.app.data.auth.Session
import com.school.app.ui.attendance.AttendanceHistoryScreen
import com.school.app.ui.attendance.AttendanceMarkScreen
import com.school.app.ui.auth.LoginScreen
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.examresult.ExamResultsScreen
import com.school.app.ui.fees.FeesScreen
import com.school.app.ui.home.HomeScreen
import com.school.app.ui.homework.HomeworkCreateScreen
import com.school.app.ui.homework.HomeworkListScreen
import com.school.app.ui.notices.NoticesScreen
import com.school.app.ui.students.ChildrenScreen
import com.school.app.ui.students.StudentDetailScreen
import com.school.app.ui.students.StudentsScreen
import com.school.app.ui.timetable.TimetableScreen
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
}

@Composable
fun AppRoot(mainViewModel: MainViewModel = hiltViewModel()) {
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()
    when (val s = sessionState) {
        SessionUi.Loading -> CenteredLoading()
        SessionUi.LoggedOut -> LoginScreen()
        is SessionUi.LoggedIn -> MainNavHost(session = s.session, onLogout = mainViewModel::logout)
    }
}

@Composable
private fun MainNavHost(session: Session, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val role = session.role

    fun optionalString(name: String) = navArgument(name) {
        type = NavType.StringType
        defaultValue = ""
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
            FeesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTICES) {
            NoticesScreen(onBack = { navController.popBackStack() })
        }
    }
}
