package com.school.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedAttendance::class,
        CachedTimetableEntry::class,
        CachedHomework::class,
        PendingAttendance::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timetableDao(): TimetableDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun pendingAttendanceDao(): PendingAttendanceDao
}
