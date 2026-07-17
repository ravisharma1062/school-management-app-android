package com.school.app

import androidx.compose.ui.graphics.Color
import com.school.app.domain.model.AttendanceStatus
import com.school.app.domain.model.BookIssueStatus
import com.school.app.domain.model.FeeStatus
import com.school.app.domain.model.LeaveStatus
import com.school.app.domain.model.SchoolStatus
import com.school.app.ui.common.color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The status -> Color mappings are plain Kotlin `when` expressions — pure and cheap to pin down. */
class StatusColorTest {

    @Test
    fun `every attendance status maps to a distinct color`() {
        val colors = AttendanceStatus.entries.map { it.color() }
        assertEquals(colors.size, colors.toSet().size)
        assertEquals(Color(0xFF15803D), AttendanceStatus.PRESENT.color())
        assertEquals(Color(0xFFB91C1C), AttendanceStatus.ABSENT.color())
    }

    @Test
    fun `every fee status maps to a distinct color`() {
        val colors = FeeStatus.entries.map { it.color() }
        assertEquals(colors.size, colors.toSet().size)
        assertEquals(Color(0xFF15803D), FeeStatus.PAID.color())
        assertEquals(Color(0xFFB91C1C), FeeStatus.OVERDUE.color())
    }

    @Test
    fun `every leave status maps to a distinct color`() {
        val colors = LeaveStatus.entries.map { it.color() }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `book issue statuses map to different colors`() {
        assertNotEquals(BookIssueStatus.ISSUED.color(), BookIssueStatus.RETURNED.color())
    }

    @Test
    fun `trial and past due school statuses share the same warning color`() {
        // Both are "needs attention soon" states, deliberately rendered identically.
        assertEquals(SchoolStatus.TRIAL.color(), SchoolStatus.PAST_DUE.color())
        assertNotEquals(SchoolStatus.ACTIVE.color(), SchoolStatus.SUSPENDED.color())
    }
}
