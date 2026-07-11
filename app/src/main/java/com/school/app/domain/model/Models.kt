package com.school.app.domain.model

// Plain Kotlin data classes mirroring the backend DTOs (see backend api-docs/openapi.json).
// Gson maps them by field name, so names must match the JSON contract exactly.

enum class Role { ADMIN, TEACHER, PARENT }

enum class TargetRole { ADMIN, TEACHER, PARENT, ALL }

enum class AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }

enum class FeeStatus { PENDING, PARTIAL, PAID, OVERDUE }

enum class LeaveType { SICK, CASUAL, OTHER }

enum class LeaveStatus { PENDING, APPROVED, REJECTED }

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
    val active: Boolean = true,
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
    val active: Boolean = true,
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

// --- Homework submissions ---
enum class HomeworkSubmissionStatus { SUBMITTED, GRADED }

data class HomeworkSubmission(
    val id: String,
    val homeworkId: String,
    val studentId: String,
    val fileName: String,
    val contentType: String,
    val status: HomeworkSubmissionStatus,
    val teacherFeedback: String? = null,
    val grade: String? = null,
    val submittedAt: String,
)

data class HomeworkSubmissionGradeRequest(
    val teacherFeedback: String? = null,
    val grade: String,
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
    val active: Boolean = true,
)

// --- Leave requests ---
data class LeaveRequest(
    val id: String,
    val requesterId: String,
    val type: LeaveType,
    val fromDate: String,
    val toDate: String,
    val reason: String? = null,
    val status: LeaveStatus,
    val reviewedBy: String? = null,
    val createdAt: String,
)

data class LeaveRequestCreateRequest(
    val type: LeaveType,
    val fromDate: String,
    val toDate: String,
    val reason: String? = null,
)

data class LeaveRequestReviewRequest(
    val status: LeaveStatus,
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

// --- Payments ---
data class PaymentInitiateRequest(val feeId: String)

data class PaymentInitiateResponse(
    val gatewayOrderId: String,
    val amountInSmallestUnit: Long,
    val currency: String,
    val gatewayKeyId: String,
)

// --- Messaging ---
data class Conversation(
    val id: String,
    val parentId: String,
    val parentName: String,
    val teacherId: String,
    val teacherName: String,
    val createdAt: String,
)

data class ConversationCreateRequest(val otherUserId: String)

data class ConversationContact(val id: String, val name: String, val email: String)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val sentAt: String,
)

data class MessageCreateRequest(val body: String)

// --- Events ---
enum class RsvpStatus { GOING, MAYBE, NOT_GOING }

data class SchoolEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val eventDate: String,
    val location: String? = null,
    val createdBy: String,
    val createdAt: String,
    val myRsvpStatus: RsvpStatus? = null,
)

data class EventCreateRequest(
    val title: String,
    val description: String? = null,
    val eventDate: String,
    val location: String? = null,
)

data class EventRsvpDto(
    val id: String,
    val eventId: String,
    val userId: String,
    val userName: String,
    val status: RsvpStatus,
    val respondedAt: String,
)

data class EventRsvpRequest(val status: RsvpStatus)

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
