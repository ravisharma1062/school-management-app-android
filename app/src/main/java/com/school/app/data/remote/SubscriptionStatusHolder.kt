package com.school.app.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transient, session-scoped subscription lifecycle signals populated by [SubscriptionStatusInterceptor]
 * from response headers/error codes on ANY API call, mirroring the web client's event-based approach.
 * Reset on logout so a fresh login starts clean.
 */
@Singleton
class SubscriptionStatusHolder @Inject constructor() {
    private val _isSuspended = MutableStateFlow(false)
    val isSuspended: StateFlow<Boolean> = _isSuspended.asStateFlow()

    private val _isPastDue = MutableStateFlow(false)
    val isPastDue: StateFlow<Boolean> = _isPastDue.asStateFlow()

    fun markSuspended() {
        _isSuspended.value = true
    }

    fun markPastDue() {
        _isPastDue.value = true
    }

    fun dismissPastDue() {
        _isPastDue.value = false
    }

    fun reset() {
        _isSuspended.value = false
        _isPastDue.value = false
    }
}
