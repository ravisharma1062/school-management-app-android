package com.school.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM cached_attendance WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun forStudent(studentId: String): List<CachedAttendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<CachedAttendance>)

    @Query("DELETE FROM cached_attendance WHERE studentId = :studentId")
    suspend fun deleteForStudent(studentId: String)

    @Transaction
    suspend fun replaceForStudent(studentId: String, rows: List<CachedAttendance>) {
        deleteForStudent(studentId)
        upsertAll(rows)
    }
}

@Dao
interface TimetableDao {
    @Query(
        "SELECT * FROM cached_timetable WHERE studentClass = :studentClass AND section = :section " +
            "ORDER BY period",
    )
    suspend fun forClass(studentClass: String, section: String): List<CachedTimetableEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<CachedTimetableEntry>)

    @Query("DELETE FROM cached_timetable WHERE studentClass = :studentClass AND section = :section")
    suspend fun deleteForClass(studentClass: String, section: String)

    @Transaction
    suspend fun replaceForClass(studentClass: String, section: String, rows: List<CachedTimetableEntry>) {
        deleteForClass(studentClass, section)
        upsertAll(rows)
    }
}

@Dao
interface HomeworkDao {
    @Query(
        "SELECT * FROM cached_homework WHERE studentClass = :studentClass AND section = :section " +
            "ORDER BY dueDate DESC",
    )
    suspend fun forClass(studentClass: String, section: String): List<CachedHomework>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<CachedHomework>)

    @Query("DELETE FROM cached_homework WHERE studentClass = :studentClass AND section = :section")
    suspend fun deleteForClass(studentClass: String, section: String)

    @Transaction
    suspend fun replaceForClass(studentClass: String, section: String, rows: List<CachedHomework>) {
        deleteForClass(studentClass, section)
        upsertAll(rows)
    }
}

@Dao
interface PendingAttendanceDao {
    @Insert
    suspend fun insertAll(rows: List<PendingAttendance>)

    @Query("SELECT * FROM pending_attendance ORDER BY queuedAt")
    suspend fun all(): List<PendingAttendance>

    @Query("SELECT COUNT(*) FROM pending_attendance")
    fun count(): Flow<Int>

    @Delete
    suspend fun delete(row: PendingAttendance)

    @Query("UPDATE pending_attendance SET lastError = :error WHERE localId = :localId")
    suspend fun setError(localId: Long, error: String?)
}
