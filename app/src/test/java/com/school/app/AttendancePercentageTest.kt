package com.school.app

import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.attendancePercentage
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendancePercentageTest {

    private fun record(status: AttendanceStatus, date: String = "2026-07-01") =
        Attendance(id = date + status.name, studentId = "s1", date = date, status = status, markedBy = "t1")

    @Test
    fun `empty history is zero percent`() {
        assertEquals(0, attendancePercentage(emptyList()))
    }

    @Test
    fun `all present is one hundred percent`() {
        val records = List(5) { record(AttendanceStatus.PRESENT, "2026-07-0${it + 1}") }
        assertEquals(100, attendancePercentage(records))
    }

    @Test
    fun `late counts as attended`() {
        val records = listOf(
            record(AttendanceStatus.PRESENT, "2026-07-01"),
            record(AttendanceStatus.LATE, "2026-07-02"),
            record(AttendanceStatus.ABSENT, "2026-07-03"),
            record(AttendanceStatus.EXCUSED, "2026-07-04"),
        )
        assertEquals(50, attendancePercentage(records))
    }

    @Test
    fun `two thirds rounds to nearest whole percent`() {
        val records = listOf(
            record(AttendanceStatus.PRESENT, "2026-07-01"),
            record(AttendanceStatus.PRESENT, "2026-07-02"),
            record(AttendanceStatus.ABSENT, "2026-07-03"),
        )
        assertEquals(67, attendancePercentage(records))
    }
}
