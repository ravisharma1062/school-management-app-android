package com.school.app.data.remote

import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceMarkRequest
import com.school.app.domain.model.AuthResponse
import com.school.app.domain.model.ExamResult
import com.school.app.domain.model.Fee
import com.school.app.domain.model.Homework
import com.school.app.domain.model.HomeworkCreateRequest
import com.school.app.domain.model.LoginRequest
import com.school.app.domain.model.Notice
import com.school.app.domain.model.PageResponse
import com.school.app.domain.model.RefreshRequest
import com.school.app.domain.model.Student
import com.school.app.domain.model.TimetableEntry
import com.school.app.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit mirror of the backend REST contract (base path /api/v1/). */
interface ApiService {

    // --- Auth ---
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): User

    // --- Students ---
    @GET("students")
    suspend fun students(
        @Query("page") page: Int,
        @Query("size") size: Int,
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

    // --- Exam results ---
    @GET("exam-results/student/{studentId}")
    suspend fun examResults(@Path("studentId") studentId: String): List<ExamResult>

    // --- Notices ---
    @GET("notices")
    suspend fun notices(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<Notice>

    // --- Fees ---
    @GET("fees/student/{studentId}")
    suspend fun feesForStudent(@Path("studentId") studentId: String): List<Fee>
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
