package com.school.app

import com.school.app.data.local.CachedAttendance
import com.school.app.data.local.CachedHomework
import com.school.app.data.local.CachedTimetableEntry
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.Homework
import com.school.app.domain.model.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/** Room cache rows convert to and from domain models without losing fields. */
class CachedEntitiesTest {

    @Test
    fun `attendance round trips through the cache row`() {
        val model = Attendance(id = "a1", studentId = "s1", date = "2026-07-16", status = AttendanceStatus.LATE, markedBy = "t1")
        assertEquals(model, CachedAttendance.from(model).toModel())
    }

    @Test
    fun `unknown persisted attendance status defaults to present`() {
        val row = CachedAttendance(id = "a1", studentId = "s1", date = "2026-07-16", status = "HOLIDAY", markedBy = "t1")
        assertEquals(AttendanceStatus.PRESENT, row.toModel().status)
    }

    @Test
    fun `timetable entry round trips through the cache row`() {
        val model = TimetableEntry(
            id = "tt1", studentClass = "5", section = "A", dayOfWeek = "MONDAY",
            period = 3, subject = "Maths", teacherId = "t9",
        )
        assertEquals(model, CachedTimetableEntry.from(model).toModel())
    }

    @Test
    fun `homework round trips through the cache row including nullable description`() {
        val withDescription = Homework(
            id = "h1", studentClass = "5", section = "A", subject = "Science",
            title = "Leaves", description = "Collect five leaves", dueDate = "2026-07-20",
            createdBy = "t1", createdAt = "2026-07-16T09:00:00Z",
        )
        assertEquals(withDescription, CachedHomework.from(withDescription).toModel())

        val withoutDescription = withDescription.copy(description = null)
        assertEquals(withoutDescription, CachedHomework.from(withoutDescription).toModel())
    }
}
