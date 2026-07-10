package com.school.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.Homework
import com.school.app.domain.model.TimetableEntry

// Room rows for offline viewing of attendance, homework and timetable,
// plus the queue of attendance marks made while offline.

@Entity(tableName = "cached_attendance", indices = [Index("studentId")])
data class CachedAttendance(
    @PrimaryKey val id: String,
    val studentId: String,
    val date: String,
    val status: String,
    val markedBy: String,
) {
    fun toModel() = Attendance(
        id = id,
        studentId = studentId,
        date = date,
        status = runCatching { AttendanceStatus.valueOf(status) }.getOrDefault(AttendanceStatus.PRESENT),
        markedBy = markedBy,
    )

    companion object {
        fun from(a: Attendance) = CachedAttendance(a.id, a.studentId, a.date, a.status.name, a.markedBy)
    }
}

@Entity(tableName = "cached_timetable", indices = [Index("studentClass", "section")])
data class CachedTimetableEntry(
    @PrimaryKey val id: String,
    val studentClass: String,
    val section: String,
    val dayOfWeek: String,
    val period: Int,
    val subject: String,
    val teacherId: String,
) {
    fun toModel() = TimetableEntry(id, studentClass, section, dayOfWeek, period, subject, teacherId)

    companion object {
        fun from(t: TimetableEntry) =
            CachedTimetableEntry(t.id, t.studentClass, t.section, t.dayOfWeek, t.period, t.subject, t.teacherId)
    }
}

@Entity(tableName = "cached_homework", indices = [Index("studentClass", "section")])
data class CachedHomework(
    @PrimaryKey val id: String,
    val studentClass: String,
    val section: String,
    val subject: String,
    val title: String,
    val description: String?,
    val dueDate: String,
    val createdBy: String,
    val createdAt: String,
) {
    fun toModel() = Homework(id, studentClass, section, subject, title, description, dueDate, createdBy, createdAt)

    companion object {
        fun from(h: Homework) = CachedHomework(
            h.id, h.studentClass, h.section, h.subject, h.title, h.description,
            h.dueDate, h.createdBy, h.createdAt,
        )
    }
}

@Entity(tableName = "pending_attendance")
data class PendingAttendance(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val studentId: String,
    val studentName: String,
    val date: String,
    val status: String,
    val queuedAt: Long,
    val lastError: String? = null,
)
