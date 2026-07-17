package com.school.app

import com.school.app.data.Outcome
import com.school.app.viewmodel.UiState
import com.school.app.viewmodel.toUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateTest {

    @Test
    fun `success maps to ready and keeps the cache flag`() {
        val fresh: Outcome<String> = Outcome.Success("x")
        assertEquals(UiState.Ready("x", fromCache = false), fresh.toUiState())

        val cached: Outcome<String> = Outcome.Success("x", fromCache = true)
        assertEquals(UiState.Ready("x", fromCache = true), cached.toUiState())
    }

    @Test
    fun `failure maps to error with the same message`() {
        val failure: Outcome<String> = Outcome.Failure("nope", isNetwork = true)
        assertEquals(UiState.Error("nope"), failure.toUiState())
    }
}
