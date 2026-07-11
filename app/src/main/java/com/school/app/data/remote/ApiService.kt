package com.school.app.data.remote

import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceMarkRequest
import com.school.app.domain.model.AuthResponse
import com.school.app.domain.model.Book
import com.school.app.domain.model.BookIssue
import com.school.app.domain.model.BusLocation
import com.school.app.domain.model.Conversation
import com.school.app.domain.model.ConversationContact
import com.school.app.domain.model.ConversationCreateRequest
import com.school.app.domain.model.EventCreateRequest
import com.school.app.domain.model.EventRsvpDto
import com.school.app.domain.model.EventRsvpRequest
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Message
import com.school.app.domain.model.MessageCreateRequest
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkCreateRequest
import com.school.app.domain.model.HomeworkSubmission
import com.school.app.domain.model.HomeworkSubmissionGradeRequest
import com.school.app.domain.model.LeaveRequest
import com.school.app.domain.model.LeaveRequestCreateRequest
import com.school.app.domain.model.LeaveRequestReviewRequest
import com.school.app.domain.model.LoginRequest
import com.school.app.domain.model.UserLanguageUpdateRequest
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.PaymentInitiateRequest
import com.school.app.domain.model.PaymentInitiateResponse
import com.school.app.domain.model.RefreshRequest
import com.school.app.domain.model.SchoolEvent
import com.school.app.domain.model.Student
import com.school.app.domain.model.StudentTransport
import com.school.app.domain.model.TimetableEntry
import com.school.app.domain.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Retrofit mirror of the backend REST contract (base path /api/v1/). */
interface ApiService {

    // --- Auth ---
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): User

    @PATCH("users/me/language")
    suspend fun updateLanguage(@Body body: UserLanguageUpdateRequest): User

    // --- Students ---
    @GET("students")
    suspend fun students(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("name") name: String? = null,
        @Query("rollNo") rollNo: String? = null,
        @Query("studentClass") studentClass: String? = null,
    ): PageResponse<Student>

    @GET("students/my-children")
    suspend fun myChildren(): List<Student>

    @GET("students/{id}")
    suspend fun student(@Path("id") id: String): Student

    // --- Attendance ---
    @POST("attendance")
    suspend fun markAttendance(@Body body: AttendanceMarkRequest): Attendance

    @GET("attendance/student/{studentId}")
    suspend fun attendanceForStudent(@Path("studentId") studentId: String): List<Attendance>

    @GET("attendance/class/{studentClass}/{section}/{date}")
    suspend fun attendanceForClass(
        @Path("studentClass") studentClass: String,
        @Path("section") section: String,
        @Path("date") date: String,
    ): List<Attendance>

    // --- Timetable ---
    @GET("timetable/{studentClass}/{section}")
    suspend fun timetable(
        @Path("studentClass") studentClass: String,
        @Path("section") section: String,
    ): List<TimetableEntry>

    // --- Homework ---
    @GET("homework/{studentClass}/{section}")
    suspend fun homework(
        @Path("studentClass") studentClass: String,
        @Path("section") section: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<Homework>

    @POST("homework")
    suspend fun createHomework(@Body body: HomeworkCreateRequest): Homework

    @Multipart
    @POST("homework/{homeworkId}/submissions")
    suspend fun submitHomework(
        @Path("homeworkId") homeworkId: String,
        @Part("studentId") studentId: RequestBody,
        @Part file: MultipartBody.Part,
    ): HomeworkSubmission

    @PATCH("homework/submissions/{id}")
    suspend fun gradeHomeworkSubmission(
        @Path("id") id: String,
        @Body body: HomeworkSubmissionGradeRequest,
    ): HomeworkSubmission

    @GET("homework/{homeworkId}/submissions")
    suspend fun submissionsByHomework(@Path("homeworkId") homeworkId: String): List<HomeworkSubmission>

    @GET("homework/submissions/student/{studentId}")
    suspend fun submissionsByStudent(@Path("studentId") studentId: String): List<HomeworkSubmission>

    @Streaming
    @GET("homework/submissions/{id}/file")
    suspend fun downloadSubmissionFile(@Path("id") id: String): ResponseBody

    // --- Exam results ---
    @GET("exam-results/student/{studentId}")
    suspend fun examResults(@Path("studentId") studentId: String): List<ExamResult>

    @Streaming
    @GET("exam-results/student/{studentId}/report-card")
    suspend fun reportCard(@Path("studentId") studentId: String): ResponseBody

    // --- Notices ---
    @GET("notices")
    suspend fun notices(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<Notice>

    // --- Fees ---
    @GET("fees/student/{studentId}")
    suspend fun feesForStudent(@Path("studentId") studentId: String): List<Fee>

    // --- Payments ---
    @POST("payments/initiate")
    suspend fun initiatePayment(@Body body: PaymentInitiateRequest): PaymentInitiateResponse

    // --- Leave requests ---
    @GET("leave-requests")
    suspend fun leaveRequests(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: String? = null,
    ): PageResponse<LeaveRequest>

    @POST("leave-requests")
    suspend fun createLeaveRequest(@Body body: LeaveRequestCreateRequest): LeaveRequest

    @PATCH("leave-requests/{id}")
    suspend fun reviewLeaveRequest(
        @Path("id") id: String,
        @Body body: LeaveRequestReviewRequest,
    ): LeaveRequest

    // --- Messaging ---
    @POST("conversations")
    suspend fun startConversation(@Body body: ConversationCreateRequest): Conversation

    @GET("conversations")
    suspend fun conversations(): List<Conversation>

    @GET("conversations/contacts")
    suspend fun conversationContacts(): List<ConversationContact>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: String, @Body body: MessageCreateRequest): Message

    @GET("conversations/{id}/messages")
    suspend fun messages(@Path("id") conversationId: String): List<Message>

    // --- Events ---
    @GET("events")
    suspend fun events(@Query("range") range: Int): List<SchoolEvent>

    @POST("events")
    suspend fun createEvent(@Body body: EventCreateRequest): SchoolEvent

    @POST("events/{id}/rsvp")
    suspend fun rsvpEvent(@Path("id") id: String, @Body body: EventRsvpRequest): EventRsvpDto

    @GET("events/{id}/rsvps")
    suspend fun eventRsvps(@Path("id") id: String): List<EventRsvpDto>

    // --- Transport ---
    @GET("transport/students/{studentId}")
    suspend fun studentTransport(@Path("studentId") studentId: String): StudentTransport

    @GET("transport/routes/{id}/location/latest")
    suspend fun busLocation(@Path("id") routeId: String): BusLocation

    // --- Library ---
    @GET("library/students/{studentId}/issues")
    suspend fun libraryIssuesForStudent(@Path("studentId") studentId: String): List<BookIssue>

    @GET("library/books")
    suspend fun libraryBooks(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String? = null,
    ): PageResponse<Book>
}

/**
 * Token refresh runs on a client WITHOUT the auth interceptor/authenticator,
 * mirroring the web client's "bare axios" refresh call, so a 401 on refresh
 * can never recurse.
 */
interface RefreshApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse
}
