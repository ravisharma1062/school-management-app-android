package com.school.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.app.data.Outcome
import com.school.app.data.repository.AttendanceRepository
import com.school.app.data.repository.StudentRepository
import com.school.app.data.sync.AttendanceSyncManager
import com.school.app.domain.model.Attendance
import com.school.app.domain.model.AttendanceMarkRequest
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Teacher's class attendance marking grid, with the offline queue behind it. */
@HiltViewModel
class AttendanceMarkViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val syncManager: AttendanceSyncManager,
) : ViewModel() {

    var studentClass by mutableStateOf("")
    var section by mutableStateOf("")
    var date by mutableStateOf(LocalDate.now())

    sealed interface Roster {
        data object Idle : Roster
        data object Loading : Roster
        data class Ready(val students: List<Student>, val alreadyMarked: Boolean) : Roster
        data class Error(val message: String) : Roster
    }

    var roster by mutableStateOf<Roster>(Roster.Idle)
        private set

    /** studentId -> selected status; defaults to PRESENT when the roster loads. */
    val statuses: SnapshotStateMap<String, AttendanceStatus> = mutableStateMapOf()

    var submitting by mutableStateOf(false)
        private set
    var resultMessage by mutableStateOf<String?>(null)
        private set

    val pendingCount: StateFlow<Int> = syncManager.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun loadRoster() {
        val cls = studentClass.trim()
        val sec = section.trim()
        if (cls.isEmpty() || sec.isEmpty()) {
            roster = Roster.Error("Enter class and section")
            return
        }
        roster = Roster.Loading
        resultMessage = null
        viewModelScope.launch {
            when (val students = studentRepository.allInClass(cls, sec)) {
                is Outcome.Failure -> roster = Roster.Error(students.message)
                is Outcome.Success -> {
                    if (students.data.isEmpty()) {
                        roster = Roster.Error("No students found in $cls-$sec")
                        return@launch
                    }
                    statuses.clear()
                    students.data.forEach { statuses[it.id] = AttendanceStatus.PRESENT }
                    // Prefill from marks already saved for this date, if any.
                    val existing = attendanceRepository.classOnDate(cls, sec, date.toString())
                    var alreadyMarked = false
                    if (existing is Outcome.Success && existing.data.isNotEmpty()) {
                        alreadyMarked = true
                        existing.data.forEach { statuses[it.studentId] = it.status }
                    }
                    roster = Roster.Ready(students.data, alreadyMarked)
                }
            }
        }
    }

    fun setStatus(studentId: String, status: AttendanceStatus) {
        statuses[studentId] = status
    }

    fun submit() {
        val current = roster as? Roster.Ready ?: return
        if (submitting) return
        submitting = true
        resultMessage = null
        viewModelScope.launch {
            val marks = current.students.map {
                AttendanceMarkRequest(
                    studentId = it.id,
                    date = date.toString(),
                    status = statuses[it.id] ?: AttendanceStatus.PRESENT,
                )
            }
            val names = current.students.associate { it.id to it.name }
            val result = attendanceRepository.submit(marks, names)
            resultMessage = buildString {
                if (result.sent > 0) append("Saved ${result.sent} marks. ")
                if (result.queued > 0) {
                    append("${result.queued} queued offline — they'll sync when you're back online. ")
                }
                if (result.failures.isNotEmpty()) {
                    append("Failed: ")
                    append(result.failures.joinToString { (name, err) -> "$name ($err)" })
                }
            }.trim().ifEmpty { "Nothing to save" }
            submitting = false
        }
    }

    fun syncNow() = syncManager.syncNow()

    fun clearResultMessage() {
        resultMessage = null
    }
}

/** Attendance history for one student (teacher or parent view). */
@HiltViewModel
class AttendanceHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])
    val studentName: String = savedStateHandle["name"] ?: ""

    var state by mutableStateOf<UiState<List<Attendance>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            state = attendanceRepository.history(studentId).toUiState()
        }
    }
}
