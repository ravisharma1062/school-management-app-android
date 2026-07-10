package com.school.app.data.sync

import com.school.app.data.repository.AttendanceRepository
import com.school.app.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flushes attendance marks queued offline whenever connectivity returns
 * (the plan's sync-on-reconnect pattern). Started once from SchoolApp.
 */
@Singleton
class AttendanceSyncManager @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val connectivityObserver: ConnectivityObserver,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val syncMutex = Mutex()

    private val _lastResult = MutableStateFlow<AttendanceRepository.SyncResult?>(null)
    val lastResult: StateFlow<AttendanceRepository.SyncResult?> = _lastResult

    val pendingCount: Flow<Int> = attendanceRepository.pendingCount

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            connectivityObserver.isOnline
                .filter { it }
                .collect { runSync() }
        }
    }

    fun syncNow() {
        scope.launch { runSync() }
    }

    private suspend fun runSync() {
        syncMutex.withLock {
            _lastResult.value = attendanceRepository.syncPending()
        }
    }
}
