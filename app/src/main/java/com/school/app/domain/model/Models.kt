package com.school.app.domain.model

// Plain Kotlin data classes mirroring the backend DTOs (see backend api-docs/openapi.json).
// Gson maps them by field name, so names must match the JSON contract exactly.

enum class Role { ADMIN, TEACHER, PARENT }

enum class TargetRole { ADMIN, TEACHER, PARENT, ALL }

enum class AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }

enum class FeeStatus { PENDING, PARTIAL, PAID, OVERDUE }

// --- Auth ---
data class LoginRequest(val email: String, val password: String)

data class RefreshRequest(val refreshToken: String)

data class AuthResponse(val accessToken: String, val refreshToken: String, val role: Role)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: Role,
    val phone: String? = null,
)

// --- Students ---
data class Student(
    val id: String,
    val name: String,
    val rollNo: String,
    val studentClass: String,
    val section: String,
    val dob: String, // yyyy-MM-dd
    val parentId: String? = null,
)

// --- Attendance ---
data class Attendance(
    val id: String,
    val studentId: String,
    val date: String,
    val status: AttendanceStatus,
    val markedBy: String,
)

data class AttendanceMarkRequest(
    val studentId: String,
    val date: String,
    val status: AttendanceStatus,
)

// --- Timetable ---
data class TimetableEntry(
    val id: String,
    val studentClass: String,
    val section: String,
    val dayOfWeek: String, // MONDAY..SUNDAY
    val period: Int,
    val subject: String,
    val teacherId: String,
)

// --- Homework ---
data class Homework(
    val id: String,
    val studentClass: String,
    val section: String,
    val subject: String,
    val title: String,
    val description: String? = null,
    val dueDate: String,
    val createdBy: String,
    val createdAt: String,
)

data class HomeworkCreateRequest(
    val studentClass: String,
    val section: String,
    val subject: String,
    val title: String,
    val description: String? = null,
    val dueDate: String,
)

// --- Exam results ---
data class ExamResult(
    val id: String,
    val studentId: String,
    val subject: String,
    val examName: String,
    val marksObtained: Double,
    val maxMarks: Double,
    val grade: String,
    val term: String,
)

// --- Notices ---
data class Notice(
    val id: String,
    val title: String,
    val description: String? = null,
    val targetRole: TargetRole,
    val createdBy: String,
    val createdAt: String,
)

// --- Fees ---
data class Fee(
    val id: String,
    val studentId: String,
    val term: String,
    val amountDue: Double,
    val amountPaid: Double,
    val status: FeeStatus,
    val dueDate: String,
)

// --- Spring Data Page<T> (subset the app uses) ---
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
)

/** Share of days the student attended (PRESENT or LATE), rounded to whole percent. */
fun attendancePercentage(records: List<Attendance>): Int {
    if (records.isEmpty()) return 0
    val attended = records.count {
        it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE
    }
    return Math.round(attended * 100f / records.size)
}
